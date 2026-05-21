# PyTorch 中文情感分类 Demo

中文短文本二分类练习项目，覆盖数据处理、模型训练、保存加载、命令行推理和 HTTP 推理。

## 内容

- `src/text_dataset.py`：分词、词表构建、文本编码
- `src/models.py`：MLP 与 TextCNN
- `src/train.py`：训练、评估、保存模型
- `src/infer.py`：命令行推理
- `src/api.py`：FastAPI 推理接口

## 安装

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
```

如果 PyTorch 安装失败，可以使用 CPU 版本：

```powershell
python -m pip install torch --index-url https://download.pytorch.org/whl/cpu
```

## 训练

```powershell
python -m src.train --model cnn --epochs 20
```

也可以训练 MLP：

```powershell
python -m src.train --model mlp --epochs 20
```

模型会保存到：

```text
artifacts/sentiment_model.pt
```

## 推理

```powershell
python -m src.infer --text "这个产品很好用" --text "物流太慢了"
```

## HTTP 接口

```powershell
uvicorn src.api:app --reload
```

访问：

```text
http://127.0.0.1:8000/
```

## 说明

数据集较小，重点展示 PyTorch 工程流程，不追求生产级模型效果。
