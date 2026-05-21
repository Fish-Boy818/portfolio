from __future__ import annotations

import csv
from collections import Counter
from pathlib import Path
from typing import Iterable

import torch
from torch.utils.data import Dataset

from src.config import PAD_ID, PAD_TOKEN, UNK_ID, UNK_TOKEN


def tokenize(text: str) -> list[str]:
    """Character-level tokenizer for a no-dependency Chinese text demo."""
    return [char.lower() for char in text.strip() if not char.isspace()]


def load_labeled_csv(path: str | Path) -> list[tuple[str, int]]:
    rows: list[tuple[str, int]] = []
    with Path(path).open("r", encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file)
        for row in reader:
            rows.append((row["text"], int(row["label"])))
    return rows


def build_vocab(
    texts: Iterable[str],
    min_freq: int = 1,
    max_size: int | None = None,
) -> dict[str, int]:
    counter: Counter[str] = Counter()
    for text in texts:
        counter.update(tokenize(text))

    vocab = {PAD_TOKEN: PAD_ID, UNK_TOKEN: UNK_ID}
    tokens = sorted(
        ((token, count) for token, count in counter.items() if count >= min_freq),
        key=lambda item: (-item[1], item[0]),
    )
    if max_size is not None:
        tokens = tokens[: max(0, max_size - len(vocab))]

    for token, _ in tokens:
        vocab[token] = len(vocab)
    return vocab


def encode_text(text: str, vocab: dict[str, int], max_len: int) -> list[int]:
    ids = [vocab.get(token, UNK_ID) for token in tokenize(text)]
    ids = ids[:max_len]
    return ids + [PAD_ID] * (max_len - len(ids))


class SentimentDataset(Dataset):
    def __init__(
        self,
        samples: list[tuple[str, int]],
        vocab: dict[str, int],
        max_len: int,
    ) -> None:
        self.samples = samples
        self.vocab = vocab
        self.max_len = max_len

    def __len__(self) -> int:
        return len(self.samples)

    def __getitem__(self, index: int) -> dict[str, torch.Tensor]:
        text, label = self.samples[index]
        input_ids = encode_text(text, self.vocab, self.max_len)
        return {
            "input_ids": torch.tensor(input_ids, dtype=torch.long),
            "label": torch.tensor(label, dtype=torch.long),
        }
