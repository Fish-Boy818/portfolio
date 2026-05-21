# PyTorch 中文情感分类 Demo

一个面向入门练习的中文短文本情感分类项目，覆盖从数据集构建、模型训练、模型保存到命令行和 HTTP 推理的完整流程。

任务：输入一段中文短文本，输出 `positive` 或 `negative`。

## 项目能力

| 模块 | 说明 |
| --- | --- |
| Tensor 基础 | 演示张量形状、运算和设备概念 |
| 数据集处理 | CSV 读取、字符级分词、词表构建、定长编码 |
| 模型结构 | 提供 MLP 与 TextCNN 两种文本分类模型 |
| 训练流程 | 前向计算、损失函数、反向传播、评估和保存 |
| 推理接口 | 支持命令行推理和 FastAPI HTTP 推理 |

## 目录结构

```text
.
├── data/
│   ├── train.csv
│   └── test.csv
├── src/
│   ├── tensor_basics.py
│   ├── text_dataset.py
│   ├── models.py
│   ├── train.py
│   ├── infer.py
│   └── api.py
├── tests/
│   └── test_text_dataset.py
├── requirements.txt
└── README.md
```

## 安装

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
```

如果默认 PyTorch 安装失败，可以按 CPU 环境安装：

```powershell
python -m pip install torch --index-url https://download.pytorch.org/whl/cpu
```

## 快速运行

查看 Tensor 基础：

```powershell
python -m src.tensor_basics
```

训练 TextCNN：

```powershell
python -m src.train --model cnn --epochs 20
```

训练 MLP：

```powershell
python -m src.train --model mlp --epochs 20
```

训练完成后会生成：

```text
artifacts/sentiment_model.pt
```

该文件保存模型参数、词表、最大文本长度、类别名称和模型配置。

## 命令行推理

```powershell
python -m src.infer --text "这个产品很好用" --text "物流太慢了"
```

示例输出：

```text
{'text': '这个产品很好用', 'label': 'positive', 'score': 0.91, ...}
{'text': '物流太慢了', 'label': 'negative', 'score': 0.88, ...}
```

## HTTP 推理

启动服务：

```powershell
uvicorn src.api:app --reload
```

打开网页：

```text
http://127.0.0.1:8000/
```

调用接口：

```powershell
curl -X POST "http://127.0.0.1:8000/predict" `
  -H "Content-Type: application/json" `
  -d "{\"text\":\"这次体验非常满意\"}"
```

## 核心文件

| 文件 | 说明 |
| --- | --- |
| `src/text_dataset.py` | 文本清洗、分词、词表构建和编码 |
| `src/models.py` | `MLPTextClassifier` 与 `TextCNNClassifier` |
| `src/train.py` | 训练循环、评估和模型保存 |
| `src/infer.py` | 加载 `.pt` 文件并进行单条文本预测 |
| `src/api.py` | FastAPI `/predict` 推理接口 |
| `tests/test_text_dataset.py` | 文本处理逻辑测试 |

## 学习路线

1. 运行 `src/tensor_basics.py`，理解 Tensor 的形状、计算和设备。
2. 阅读 `src/text_dataset.py`，理解原始文本如何变成模型输入。
3. 对比 `src/models.py` 中 MLP 和 TextCNN 的结构差异。
4. 运行 `src/train.py`，观察 loss 和 accuracy。
5. 运行 `src/infer.py` 和 `src/api.py`，理解模型部署时的加载与调用方式。

## 说明

这个 Demo 使用小规模样例数据，重点是展示 PyTorch 工程链路，不追求生产级模型效果。需要提升效果时，可以扩充数据集、引入分词器、增加验证集和调参流程。
