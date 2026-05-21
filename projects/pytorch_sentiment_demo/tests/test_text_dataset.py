from src.text_dataset import build_vocab, encode_text, tokenize


def test_tokenize_removes_spaces():
    assert tokenize("好 用") == ["好", "用"]


def test_encode_text_pads_to_max_len():
    vocab = build_vocab(["好用"])
    assert encode_text("好", vocab, max_len=3) == [vocab["好"], 0, 0]
