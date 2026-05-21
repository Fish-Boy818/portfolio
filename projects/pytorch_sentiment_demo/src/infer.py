from __future__ import annotations

import argparse
from pathlib import Path

import torch

from src.models import create_model
from src.text_dataset import encode_text


def load_model(model_path: str | Path, device: str | None = None):
    runtime_device = torch.device(device or ("cuda" if torch.cuda.is_available() else "cpu"))
    checkpoint = torch.load(model_path, map_location=runtime_device)
    config = checkpoint["model_config"]
    model = create_model(
        model_name=checkpoint["model_name"],
        vocab_size=len(checkpoint["vocab"]),
        num_classes=len(checkpoint["class_names"]),
        embed_dim=config["embed_dim"],
        hidden_dim=config["hidden_dim"],
        dropout=config["dropout"],
    )
    model.load_state_dict(checkpoint["state_dict"])
    model.to(runtime_device)
    model.eval()
    return model, checkpoint, runtime_device


def predict(text: str, model, checkpoint: dict, device: torch.device) -> dict:
    input_ids = encode_text(text, checkpoint["vocab"], checkpoint["max_len"])
    tensor = torch.tensor([input_ids], dtype=torch.long, device=device)
    with torch.no_grad():
        probabilities = torch.softmax(model(tensor), dim=1).squeeze(0)
    class_id = int(probabilities.argmax().item())
    return {
        "text": text,
        "label": checkpoint["class_names"][class_id],
        "score": float(probabilities[class_id].item()),
        "probabilities": {
            name: float(probabilities[index].item())
            for index, name in enumerate(checkpoint["class_names"])
        },
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run sentiment inference.")
    parser.add_argument("--model-path", type=Path, default=Path("artifacts/sentiment_model.pt"))
    parser.add_argument("--text", action="append", required=True)
    parser.add_argument("--device", choices=["cpu", "cuda"], default=None)
    return parser.parse_args()


if __name__ == "__main__":
    args = parse_args()
    loaded_model, loaded_checkpoint, loaded_device = load_model(args.model_path, args.device)
    for item in args.text:
        result = predict(item, loaded_model, loaded_checkpoint, loaded_device)
        print(result)
