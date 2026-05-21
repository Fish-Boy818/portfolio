from __future__ import annotations

import torch
from torch import nn
from torch.nn import functional as F

from src.config import PAD_ID


class MLPTextClassifier(nn.Module):
    """Embedding average pooling + MLP classifier."""

    def __init__(
        self,
        vocab_size: int,
        num_classes: int,
        embed_dim: int = 64,
        hidden_dim: int = 64,
        dropout: float = 0.2,
    ) -> None:
        super().__init__()
        self.embedding = nn.Embedding(vocab_size, embed_dim, padding_idx=PAD_ID)
        self.classifier = nn.Sequential(
            nn.Linear(embed_dim, hidden_dim),
            nn.ReLU(),
            nn.Dropout(dropout),
            nn.Linear(hidden_dim, num_classes),
        )

    def forward(self, input_ids: torch.Tensor) -> torch.Tensor:
        embedded = self.embedding(input_ids)
        mask = input_ids.ne(PAD_ID).unsqueeze(-1)
        lengths = mask.sum(dim=1).clamp(min=1)
        pooled = (embedded * mask).sum(dim=1) / lengths
        return self.classifier(pooled)


class TextCNNClassifier(nn.Module):
    """Simple TextCNN for short text classification."""

    def __init__(
        self,
        vocab_size: int,
        num_classes: int,
        embed_dim: int = 64,
        num_filters: int = 64,
        kernel_sizes: tuple[int, ...] = (2, 3, 4),
        dropout: float = 0.2,
    ) -> None:
        super().__init__()
        self.embedding = nn.Embedding(vocab_size, embed_dim, padding_idx=PAD_ID)
        self.convs = nn.ModuleList(
            nn.Conv1d(embed_dim, num_filters, kernel_size=size)
            for size in kernel_sizes
        )
        self.dropout = nn.Dropout(dropout)
        self.fc = nn.Linear(num_filters * len(kernel_sizes), num_classes)

    def forward(self, input_ids: torch.Tensor) -> torch.Tensor:
        embedded = self.embedding(input_ids).transpose(1, 2)
        features = []
        for conv in self.convs:
            activated = F.relu(conv(embedded))
            pooled = F.max_pool1d(activated, kernel_size=activated.size(2)).squeeze(2)
            features.append(pooled)
        return self.fc(self.dropout(torch.cat(features, dim=1)))


def create_model(
    model_name: str,
    vocab_size: int,
    num_classes: int,
    embed_dim: int,
    hidden_dim: int,
    dropout: float,
) -> nn.Module:
    if model_name == "mlp":
        return MLPTextClassifier(
            vocab_size=vocab_size,
            num_classes=num_classes,
            embed_dim=embed_dim,
            hidden_dim=hidden_dim,
            dropout=dropout,
        )
    if model_name == "cnn":
        return TextCNNClassifier(
            vocab_size=vocab_size,
            num_classes=num_classes,
            embed_dim=embed_dim,
            num_filters=hidden_dim,
            dropout=dropout,
        )
    raise ValueError(f"Unknown model name: {model_name}")
