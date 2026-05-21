import torch


def main() -> None:
    x = torch.tensor([[1.0, 2.0], [3.0, 4.0]])
    y = torch.ones_like(x)
    print("x =", x)
    print("shape =", x.shape)
    print("x + y =", x + y)
    print("matrix multiply =", x @ y)
    print("mean =", x.mean())
    print("cuda available =", torch.cuda.is_available())


if __name__ == "__main__":
    main()
