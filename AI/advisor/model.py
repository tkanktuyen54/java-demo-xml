"""A decoder-only Transformer built from PyTorch layers, with random initial weights.

UTF-8 bytes avoid an externally pretrained tokenizer. This is a research model,
not a pretrained general-purpose LLM. Its constrained intent scores can guide advice.
"""

from dataclasses import asdict, dataclass
from pathlib import Path

import torch
from torch import nn
from torch.nn import functional as F

BOS, EOS, PAD = 256, 257, 258
INTENTS = ("recommend", "price", "syllabus", "open", "policy", "greeting")
INTENT_START = 259
VOCAB_SIZE = INTENT_START + len(INTENTS)


def encode(text: str) -> list:
  return list(text.encode("utf-8"))


def decode(tokens: list) -> str:
  return bytes(token for token in tokens if token < 256).decode("utf-8", errors="replace")


def prompt(question: str) -> str:
  return f"Hỏi: {question}\nÝ định: "


@dataclass
class ModelConfig:
  context: int = 256
  width: int = 64
  heads: int = 4
  layers: int = 2
  dropout: float = 0.1


class CausalBlock(nn.Module):
  def __init__(self, config: ModelConfig):
    super().__init__()
    self.heads = config.heads
    self.dropout = config.dropout
    self.norm1 = nn.LayerNorm(config.width)
    self.qkv = nn.Linear(config.width, 3 * config.width)
    self.output = nn.Linear(config.width, config.width)
    self.norm2 = nn.LayerNorm(config.width)
    self.mlp = nn.Sequential(
      nn.Linear(config.width, 4 * config.width),
      nn.GELU(),
      nn.Linear(4 * config.width, config.width),
    )

  def forward(self, values):
    batch, length, width = values.shape
    query, key, value = self.qkv(self.norm1(values)).chunk(3, dim=-1)
    query, key, value = [
      tensor.view(batch, length, self.heads, width // self.heads).transpose(1, 2)
      for tensor in (query, key, value)
    ]
    attended = F.scaled_dot_product_attention(
      query, key, value, is_causal=True, dropout_p=self.dropout if self.training else 0.0
    )
    values = values + self.output(attended.transpose(1, 2).contiguous().view(batch, length, width))
    return values + self.mlp(self.norm2(values))


class TinyGPT(nn.Module):
  def __init__(self, config: ModelConfig):
    super().__init__()
    if config.width % config.heads or config.context < 32:
      raise ValueError("width phải chia hết cho heads; context tối thiểu 32.")
    self.config = config
    self.tokens = nn.Embedding(VOCAB_SIZE, config.width)
    self.positions = nn.Embedding(config.context, config.width)
    self.blocks = nn.Sequential(*[CausalBlock(config) for _ in range(config.layers)])
    self.norm = nn.LayerNorm(config.width)
    self.head = nn.Linear(config.width, VOCAB_SIZE, bias=False)
    self.apply(self._initialize)

  @staticmethod
  def _initialize(module):
    if isinstance(module, (nn.Linear, nn.Embedding)):
      nn.init.normal_(module.weight, mean=0, std=0.02)
      if isinstance(module, nn.Linear) and module.bias is not None:
        nn.init.zeros_(module.bias)

  def forward(self, token_ids):
    length = token_ids.shape[1]
    if length > self.config.context:
      raise ValueError("Vượt quá context của mô hình.")
    embedded = self.tokens(token_ids) + self.positions(
      torch.arange(length, device=token_ids.device)
    )
    return self.head(self.norm(self.blocks(embedded)))

  @torch.inference_mode()
  def generate(self, text: str, max_new_tokens: int = 100) -> str:
    self.eval()
    device = next(self.parameters()).device
    sequence = [BOS] + encode(text)
    generated = []
    for _ in range(max_new_tokens):
      inputs = torch.tensor([sequence[-self.config.context :]], device=device)
      token = int(self(inputs)[0, -1].argmax())
      if token >= 256:
        break
      sequence.append(token)
      generated.append(token)
    return decode(generated)

  @torch.inference_mode()
  def intent(self, question: str):
    self.eval()
    device = next(self.parameters()).device
    prefix = [BOS] + encode(prompt(question))
    prefix = prefix[-(self.config.context - 1) :]
    logits = self(torch.tensor([prefix], device=device))[0, -1]
    probabilities = logits[INTENT_START:VOCAB_SIZE].softmax(0)
    index = int(probabilities.argmax())
    return INTENTS[index], float(probabilities[index])


def save_model(model: TinyGPT, path: Path, metadata: dict):
  path.parent.mkdir(parents=True, exist_ok=True)
  torch.save(
    {"config": asdict(model.config), "state_dict": model.state_dict(), "metadata": metadata}, path
  )


def load_model(path: Path, device="cpu"):
  checkpoint = torch.load(path, map_location=device, weights_only=True)
  model = TinyGPT(ModelConfig(**checkpoint["config"]))
  model.load_state_dict(checkpoint["state_dict"])
  return model.to(device).eval(), checkpoint.get("metadata", {})
