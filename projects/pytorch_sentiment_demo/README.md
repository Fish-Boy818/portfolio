# PyTorch 中文情感分类小项目

这是一个面向入门练习的完整 PyTorch 项目，覆盖：

- 张量 `Tensor`
- `Dataset` / `DataLoader`
- 简单 MLP 与 CNN 模型
- 训练流程
- 模型保存和加载
- 命令行推理
- HTTP 推理接口

项目任务：输入一段中文短文本，判断情感是 `positive` 还是 `negative`。

## 目录结构

```text
pytorch_sentiment_demo/
  data/
    train.csv
    test.csv
  src/
    tensor_basics.py
    text_dataset.py
    models.py
    train.py
    infer.py
    api.py
  tests/
    test_text_dataset.py
  requirements.txt
```

## 安装依赖

建议先创建虚拟环境：

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
```

如果默认 PyTorch 安装失败，可以按你的 CUDA/CPU 环境从官方页面选择命令：

```powershell
python -m pip install torch --index-url https://download.pytorch.org/whl/cpu
```

## 1. Tensor 基础

```powershell
python -m src.tensor_basics
```

## 2. 训练模型

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

这个文件里保存了模型参数、词表、最大文本长度、类别名称和模型配置。

## 3. 命令行推理

```powershell
python -m src.infer --text "这个产品很好用" --text "物流太慢了"
```

示例输出：

```text
{'text': '这个产品很好用', 'label': 'positive', 'score': 0.91, ...}
{'text': '物流太慢了', 'label': 'negative', 'score': 0.88, ...}
```

## 4. HTTP 推理接口

启动接口：

```powershell
uvicorn src.api:app --reload
```

打开中文网页：

```text
http://127.0.0.1:8000/
```

调用接口：

```powershell
curl -X POST "http://127.0.0.1:8000/predict" `
  -H "Content-Type: application/json" `
  -d "{\"text\":\"这次体验非常满意\"}"
```

## 关键代码说明

- `src/text_dataset.py`：读取 CSV，构建词表，把文本编码成固定长度 Tensor。
- `src/models.py`：实现 `MLPTextClassifier` 和 `TextCNNClassifier`。
- `src/train.py`：标准训练循环：前向计算、损失函数、反向传播、参数更新、评估、保存模型。
- `src/infer.py`：加载 `.pt` 文件并进行单条文本预测。
- `src/api.py`：用 FastAPI 暴露 `/predict` 推理接口。

## 学习路线

1. 先运行 `src/tensor_basics.py`，理解 Tensor 的形状、计算和设备。
2. 看 `src/text_dataset.py`，理解 Dataset 如何把原始文本变成模型输入。
3. 看 `src/models.py`，对比 MLP 和 CNN 的结构差异。
4. 运行 `src/train.py`，观察 loss 和 accuracy。
5. 运行 `src/infer.py` 和 `src/api.py`，理解模型部署时的加载与调用方式。
