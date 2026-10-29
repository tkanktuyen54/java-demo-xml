"""CPU/GPU training from random weights; checkpoints can be resumed for later training."""

import argparse
import hashlib
import json
import math
import random
from pathlib import Path

import torch
from torch.nn import functional as F

from .model import (
  BOS,
  EOS,
  PAD,
  INTENTS,
  INTENT_START,
  ModelConfig,
  TinyGPT,
  encode,
  load_model,
  prompt,
  save_model,
)


def examples(path: Path, task: str, context: int):
  if task == "text":
    documents = [
      text.strip() for text in path.read_text(encoding="utf-8").split("\n\n") if text.strip()
    ]
    return [(text, "", text) for text in documents]
  result = []
  for line in path.read_text(encoding="utf-8").splitlines():
    if not line.strip():
      continue
    row = json.loads(line)
    if task == "intents":
      if row["intent"] not in INTENTS:
        raise ValueError(f"Ý định không hợp lệ: {row['intent']}")
      result.append((row["question"], prompt(row["question"]), row["intent"]))
    else:
      history = ""
      group = json.dumps(row["messages"], ensure_ascii=False, sort_keys=True)
      for turn in row["messages"]:
        if turn["role"] == "assistant" and history:
          result.append((group, history + "<assistant>\n", turn["content"]))
        history += f"<{turn['role']}>\n{turn['content']}\n"
  return result


def split_examples(rows: list, seed: int):
  train, validation = [], []
  seen = set()
  for key, prefix, completion in rows:
    normalized = " ".join(key.lower().split())
    identity = (normalized, prefix, completion)
    if identity in seen:
      continue
    seen.add(identity)
    digest = hashlib.sha256(f"{seed}:{normalized}".encode()).digest()
    (validation if digest[0] < 51 else train).append((key, prefix, completion))
  if len(train) < 5 or len(validation) < 2:
    raise ValueError(
      "Cần thêm dữ liệu độc lập: ít nhất 5 mẫu train và 2 mẫu validation sau khi chia theo hash."
    )
  return train, validation


def tensors(rows: list, context: int, task: str):
  sequences = []
  for _, prefix, completion in rows:
    if task == "text":
      tokens = [BOS] + encode(completion) + [EOS]
      for start in range(0, len(tokens) - 1, context):
        part = tokens[start : start + context + 1]
        if len(part) > 1:
          sequences.append((part[:-1], part[1:]))
    else:
      target = (
        [INTENT_START + INTENTS.index(completion), EOS]
        if task == "intents"
        else (encode(completion) + [EOS])[: context // 2]
      )
      prefix_tokens = ([BOS] + encode(prefix))[-(context + 1 - len(target)) :]
      tokens = prefix_tokens + target
      labels = [-100] * (len(prefix_tokens) - 1) + target
      sequences.append((tokens[:-1], labels))
  return sequences


def batch(samples: list, device):
  length = max(len(sample[0]) for sample in samples)
  inputs = torch.full((len(samples), length), PAD, dtype=torch.long, device=device)
  labels = torch.full((len(samples), length), -100, dtype=torch.long, device=device)
  for index, (tokens, targets) in enumerate(samples):
    inputs[index, : len(tokens)] = torch.tensor(tokens, device=device)
    labels[index, : len(targets)] = torch.tensor(targets, device=device)
  return inputs, labels


@torch.inference_mode()
def evaluate(model, samples, device, batch_size):
  model.eval()
  total_loss, count = 0.0, 0
  for start in range(0, len(samples), batch_size):
    inputs, targets = batch(samples[start : start + batch_size], device)
    loss = F.cross_entropy(
      model(inputs).flatten(0, 1), targets.flatten(), ignore_index=-100, reduction="sum"
    )
    total_loss += float(loss)
    count += int((targets != -100).sum())
  return total_loss / count


def main():
  parser = argparse.ArgumentParser(
    description="Huấn luyện TinyGPT từ đầu hoặc tiếp tục từ checkpoint."
  )
  parser.add_argument("--data", type=Path, required=True)
  parser.add_argument("--task", choices=["intents", "text", "chat"], default="intents")
  parser.add_argument("--output", type=Path, default=Path("artifacts/intents.pt"))
  parser.add_argument("--resume", type=Path)
  parser.add_argument("--epochs", type=int, default=20)
  parser.add_argument("--batch-size", type=int, default=8)
  parser.add_argument("--context", type=int, default=256)
  parser.add_argument("--width", type=int, default=64)
  parser.add_argument("--layers", type=int, default=2)
  parser.add_argument("--heads", type=int, default=4)
  parser.add_argument("--device", default="cpu", choices=["cpu", "cuda", "mps"])
  parser.add_argument("--seed", type=int, default=42)
  args = parser.parse_args()
  if min(args.epochs, args.batch_size, args.width, args.layers, args.heads) < 1:
    parser.error("Các tham số huấn luyện phải lớn hơn 0.")
  random.seed(args.seed)
  torch.manual_seed(args.seed)
  torch.set_num_threads(2)
  model = (
    load_model(args.resume, args.device)[0]
    if args.resume
    else TinyGPT(
      ModelConfig(context=args.context, width=args.width, layers=args.layers, heads=args.heads)
    ).to(args.device)
  )
  train_rows, valid_rows = split_examples(
    examples(args.data, args.task, model.config.context), args.seed
  )
  training = tensors(train_rows, model.config.context, args.task)
  validation = tensors(valid_rows, model.config.context, args.task)
  optimizer = torch.optim.AdamW(model.parameters(), lr=0.001, weight_decay=0.01)
  print(
    json.dumps(
      {
        "parameters": sum(parameter.numel() for parameter in model.parameters()),
        "trainExamples": len(training),
        "validationExamples": len(validation),
        "device": args.device,
      }
    )
  )
  best = float("inf")
  for epoch in range(args.epochs):
    model.train()
    random.shuffle(training)
    for start in range(0, len(training), args.batch_size):
      inputs, targets = batch(training[start : start + args.batch_size], args.device)
      loss = F.cross_entropy(model(inputs).flatten(0, 1), targets.flatten(), ignore_index=-100)
      optimizer.zero_grad(set_to_none=True)
      loss.backward()
      torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
      optimizer.step()
    valid_loss = evaluate(model, validation, args.device, args.batch_size)
    metrics = {
      "task": args.task,
      "epoch": epoch + 1,
      "validationLoss": valid_loss,
      "perplexity": math.exp(min(valid_loss, 50)),
      "seed": args.seed,
      "trainExamples": len(training),
      "validationExamples": len(validation),
    }
    if args.task == "intents":
      metrics["validationIntentAccuracy"] = sum(
        model.intent(key)[0] == label for key, _, label in valid_rows
      ) / len(valid_rows)
    if valid_loss < best:
      best = valid_loss
      save_model(model, args.output, metrics)
    print(json.dumps(metrics), flush=True)
  print(f"Đã lưu checkpoint có validation loss tốt nhất: {args.output}")


if __name__ == "__main__":
  main()
