from __future__ import annotations

import os
from pathlib import Path

from fastapi import FastAPI, HTTPException
from fastapi.responses import HTMLResponse
from pydantic import BaseModel

from src.infer import load_model, predict


class PredictRequest(BaseModel):
    text: str


MODEL_PATH = Path(os.getenv("MODEL_PATH", "artifacts/sentiment_model.pt"))
LABEL_ZH = {"positive": "积极", "negative": "消极"}

app = FastAPI(title="中文情感分析演示")
model = None
checkpoint = None
device = None


@app.on_event("startup")
def startup() -> None:
    global model, checkpoint, device
    if MODEL_PATH.exists():
        model, checkpoint, device = load_model(MODEL_PATH)


@app.get("/", response_class=HTMLResponse)
def index() -> str:
    return """
<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>中文情感分析</title>
  <style>
    * { box-sizing: border-box; }
    body {
      margin: 0;
      font-family: "Microsoft YaHei", "PingFang SC", Arial, sans-serif;
      background: #f6f7f9;
      color: #1f2937;
    }
    main {
      width: min(880px, calc(100% - 32px));
      margin: 56px auto;
    }
    h1 {
      margin: 0 0 8px;
      font-size: 32px;
      font-weight: 700;
    }
    p {
      margin: 0 0 24px;
      color: #4b5563;
      line-height: 1.7;
    }
    .panel {
      background: #ffffff;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      padding: 24px;
      box-shadow: 0 12px 30px rgba(31, 41, 55, 0.08);
    }
    label {
      display: block;
      margin-bottom: 8px;
      font-weight: 600;
    }
    textarea {
      width: 100%;
      min-height: 140px;
      padding: 14px;
      border: 1px solid #cfd6df;
      border-radius: 6px;
      resize: vertical;
      font: inherit;
      line-height: 1.6;
      outline: none;
    }
    textarea:focus {
      border-color: #2563eb;
      box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
    }
    .actions {
      display: flex;
      gap: 12px;
      align-items: center;
      margin-top: 16px;
    }
    button {
      border: 0;
      border-radius: 6px;
      padding: 11px 18px;
      background: #2563eb;
      color: white;
      font: inherit;
      font-weight: 600;
      cursor: pointer;
    }
    button:disabled {
      background: #9ca3af;
      cursor: wait;
    }
    .hint {
      color: #6b7280;
      font-size: 14px;
    }
    .result {
      margin-top: 20px;
      padding: 16px;
      border-radius: 6px;
      background: #f9fafb;
      border: 1px solid #e5e7eb;
      min-height: 62px;
      line-height: 1.7;
    }
    .badge {
      display: inline-block;
      min-width: 52px;
      text-align: center;
      border-radius: 999px;
      padding: 2px 10px;
      font-weight: 700;
    }
    .positive { background: #dcfce7; color: #166534; }
    .negative { background: #fee2e2; color: #991b1b; }
    .error { color: #b91c1c; }
  </style>
</head>
<body>
  <main>
    <h1>中文情感分析</h1>
    <p>输入一段中文短文本，模型会判断它偏积极还是偏消极。</p>
    <section class="panel">
      <label for="text">待分析文本</label>
      <textarea id="text">这个产品很好用，物流也很快</textarea>
      <div class="actions">
        <button id="submit" type="button">开始分析</button>
        <span class="hint">示例：这个产品很好用 / 物流太慢了</span>
      </div>
      <div id="result" class="result">结果会显示在这里。</div>
    </section>
  </main>
  <script>
    const button = document.querySelector("#submit");
    const text = document.querySelector("#text");
    const result = document.querySelector("#result");

    const labelName = { positive: "积极", negative: "消极" };

    button.addEventListener("click", async () => {
      const value = text.value.trim();
      if (!value) {
        result.innerHTML = '<span class="error">请先输入一段文本。</span>';
        return;
      }

      button.disabled = true;
      button.textContent = "分析中";
      result.textContent = "正在调用模型...";

      try {
        const response = await fetch("/predict", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ text: value })
        });
        const data = await response.json();
        if (!response.ok) {
          throw new Error(data.detail || "请求失败");
        }

        const label = labelName[data.label] || data.label;
        const score = (data.score * 100).toFixed(2);
        const cssClass = data.label === "positive" ? "positive" : "negative";
        result.innerHTML = `
          预测结果：<span class="badge ${cssClass}">${label}</span><br>
          置信度：${score}%<br>
          原文：${data.text}
        `;
      } catch (error) {
        result.innerHTML = `<span class="error">${error.message}</span>`;
      } finally {
        button.disabled = false;
        button.textContent = "开始分析";
      }
    });
  </script>
</body>
</html>
"""


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/predict")
def predict_api(request: PredictRequest) -> dict:
    if model is None or checkpoint is None or device is None:
        raise HTTPException(
            status_code=503,
            detail="模型文件不存在，请先运行：python -m src.train --model cnn --epochs 20",
        )
    result = predict(request.text, model, checkpoint, device)
    result["label_zh"] = LABEL_ZH.get(result["label"], result["label"])
    return result
