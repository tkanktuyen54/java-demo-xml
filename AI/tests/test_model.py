import torch

from advisor.model import ModelConfig, TinyGPT, decode, encode, load_model, save_model
from advisor.train import batch, split_examples, tensors, examples


def test_utf8_tokenizer_and_causal_attention():
  assert decode(encode("Khóa học tiếng Việt")) == "Khóa học tiếng Việt"
  torch.manual_seed(7)
  model = TinyGPT(ModelConfig(context=64, width=16, heads=2, layers=1, dropout=0)).eval()
  first = torch.tensor([[1, 2, 3, 4, 5]])
  second = torch.tensor([[1, 2, 3, 90, 91]])
  # Changing future tokens cannot change predictions for the prefix.
  assert torch.allclose(model(first)[:, :3], model(second)[:, :3], atol=1e-6)


def test_checkpoint_round_trip_and_training_update(tmp_path):
  torch.manual_seed(7)
  model = TinyGPT(ModelConfig(context=64, width=16, heads=2, layers=1, dropout=0))
  samples = tensors([("Python", "Hỏi: Python\nÝ định: ", "recommend")], 64, "intents")
  inputs, targets = batch(samples, "cpu")
  optimizer = torch.optim.AdamW(model.parameters(), lr=0.01)
  before = model.head.weight.detach().clone()
  loss = torch.nn.functional.cross_entropy(
    model(inputs).flatten(0, 1), targets.flatten(), ignore_index=-100
  )
  loss.backward()
  optimizer.step()
  assert not torch.equal(before, model.head.weight)
  path = tmp_path / "model.pt"
  save_model(model, path, {"task": "intents"})
  restored, metadata = load_model(path)
  assert metadata["task"] == "intents"
  assert torch.allclose(model.eval()(inputs), restored(inputs))
  assert len(restored.generate("Python", max_new_tokens=3)) <= 3


def test_train_validation_split_has_no_duplicate_questions():
  rows = [(f"Question {index}", "prompt", "recommend") for index in range(60)]
  train, validation = split_examples(rows + rows, 42)
  assert len(train) + len(validation) == 60
  assert not set(row[0] for row in train) & set(row[0] for row in validation)


def test_chat_split_keeps_whole_conversations_together(tmp_path):
  import json

  path = tmp_path / "chat.jsonl"
  rows = []
  for index in range(30):
    rows.append(
      json.dumps(
        {
          "messages": [
            {"role": "user", "content": f"Hỏi khóa học {index}"},
            {"role": "assistant", "content": "Bạn muốn học kỹ năng gì?"},
            {"role": "user", "content": "Python"},
            {"role": "assistant", "content": "Có khóa Python cơ bản."},
          ]
        },
        ensure_ascii=False,
      )
    )
  path.write_text("\n".join(rows), encoding="utf-8")
  data = examples(path, "chat", 256)
  assert len(data) == 60
  train, validation = split_examples(data, 42)
  assert not set(row[0] for row in train) & set(row[0] for row in validation)
  assert all(prefix.endswith("<assistant>\n") for _, prefix, _ in data)
  assert "<assistant>\n<assistant>" not in data[1][1]


def test_plain_text_chunks_preserve_next_token_targets():
  samples = tensors([("doc", "", "Python " * 50)], 64, "text")
  assert len(samples) > 1
  for inputs, targets in samples:
    assert inputs[1:] == targets[:-1]
    assert len(inputs) <= 64
