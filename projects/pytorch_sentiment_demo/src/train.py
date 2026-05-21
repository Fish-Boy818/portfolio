from __future__ import annotations

import argparse
import copy
import random
from pathlib import Path

import torch
from torch import nn
from torch.utils.data import DataLoader

from src.config import (
    ARTIFACT_DIR,
    DATA_DIR,
    DEFAULT_CLASS_NAMES,
    DEFAULT_MAX_LEN,
)
from src.models import create_model
from src.text_dataset import SentimentDataset, build_vocab, load_labeled_csv


def set_seed(seed: int) -> None:
    random.seed(seed)
    torch.manual_seed(seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(seed)


def evaluate(model: nn.Module, dataloader: DataLoader, device: torch.device) -> float:
    model.eval()
    correct = 0
    total = 0
    with torch.no_grad():
        for batch in dataloader:
            input_ids = batch["input_ids"].to(device)
            labels = batch["label"].to(device)
            logits = model(input_ids)
            predictions = logits.argmax(dim=1)
            correct += (predictions == labels).sum().item()
            total += labels.numel()
    return correct / max(total, 1)


def train(args: argparse.Namespace) -> Path:
    set_seed(args.seed)
    device = torch.device(args.device or ("cuda" if torch.cuda.is_available() else "cpu"))

    train_samples = load_labeled_csv(args.train_path)
    test_samples = load_labeled_csv(args.test_path)
    vocab = build_vocab(text for text, _ in train_samples)

    train_dataset = SentimentDataset(train_samples, vocab, args.max_len)
    test_dataset = SentimentDataset(test_samples, vocab, args.max_len)
    train_loader = DataLoader(train_dataset, batch_size=args.batch_size, shuffle=True)
    test_loader = DataLoader(test_dataset, batch_size=args.batch_size)

    model = create_model(
        model_name=args.model,
        vocab_size=len(vocab),
        num_classes=len(DEFAULT_CLASS_NAMES),
        embed_dim=args.embed_dim,
        hidden_dim=args.hidden_dim,
        dropout=args.dropout,
    ).to(device)

    criterion = nn.CrossEntropyLoss()
    optimizer = torch.optim.Adam(model.parameters(), lr=args.lr)

    best_acc = 0.0
    best_epoch = 0
    best_state_dict = copy.deepcopy(model.state_dict())
    for epoch in range(1, args.epochs + 1):
        model.train()
        total_loss = 0.0
        for batch in train_loader:
            input_ids = batch["input_ids"].to(device)
            labels = batch["label"].to(device)

            optimizer.zero_grad()
            logits = model(input_ids)
            loss = criterion(logits, labels)
            loss.backward()
            optimizer.step()

            total_loss += loss.item() * labels.size(0)

        train_loss = total_loss / len(train_dataset)
        test_acc = evaluate(model, test_loader, device)
        if test_acc >= best_acc:
            best_acc = test_acc
            best_epoch = epoch
            best_state_dict = copy.deepcopy(model.state_dict())
        print(
            f"epoch={epoch:02d} "
            f"train_loss={train_loss:.4f} "
            f"test_acc={test_acc:.4f}"
        )

    ARTIFACT_DIR.mkdir(parents=True, exist_ok=True)
    output_path = Path(args.output)
    if not output_path.is_absolute():
        output_path = ARTIFACT_DIR / output_path

    checkpoint = {
        "model_name": args.model,
        "state_dict": best_state_dict,
        "vocab": vocab,
        "max_len": args.max_len,
        "class_names": DEFAULT_CLASS_NAMES,
        "model_config": {
            "embed_dim": args.embed_dim,
            "hidden_dim": args.hidden_dim,
            "dropout": args.dropout,
        },
        "metrics": {"best_test_acc": best_acc, "best_epoch": best_epoch},
    }
    torch.save(checkpoint, output_path)
    print(f"saved best model from epoch {best_epoch} to {output_path}")
    return output_path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Train a small PyTorch sentiment model.")
    parser.add_argument("--model", choices=["mlp", "cnn"], default="cnn")
    parser.add_argument("--train-path", type=Path, default=DATA_DIR / "train.csv")
    parser.add_argument("--test-path", type=Path, default=DATA_DIR / "test.csv")
    parser.add_argument("--output", type=Path, default=Path("sentiment_model.pt"))
    parser.add_argument("--max-len", type=int, default=DEFAULT_MAX_LEN)
    parser.add_argument("--embed-dim", type=int, default=64)
    parser.add_argument("--hidden-dim", type=int, default=64)
    parser.add_argument("--dropout", type=float, default=0.2)
    parser.add_argument("--batch-size", type=int, default=8)
    parser.add_argument("--epochs", type=int, default=20)
    parser.add_argument("--lr", type=float, default=1e-3)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--device", choices=["cpu", "cuda"], default=None)
    return parser.parse_args()


if __name__ == "__main__":
    train(parse_args())
