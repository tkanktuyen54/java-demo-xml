import argparse
from pathlib import Path

from .model import load_model


def main():
  parser = argparse.ArgumentParser(description="Sinh thử văn bản từ TinyGPT nghiên cứu.")
  parser.add_argument("--checkpoint", type=Path, required=True)
  parser.add_argument("--prompt", required=True)
  parser.add_argument("--max-tokens", type=int, default=100)
  args = parser.parse_args()
  if not 1 <= args.max_tokens <= 2000:
    parser.error("max-tokens phải từ 1 đến 2000.")
  model, _ = load_model(args.checkpoint)
  print(model.generate(args.prompt, args.max_tokens))


if __name__ == "__main__":
  main()
