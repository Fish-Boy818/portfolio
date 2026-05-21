# 抖音来客账号管理与数据采集工具

通过 Selenium 登录抖音来客后台，按账号列表采集数据并生成报表，可选推送到企业微信机器人。

公开版本不包含真实账号、密码、Webhook 和业务数据。

## 文件

- `popularize.py`：主采集脚本
- `account_manager.py`：账号管理与提取逻辑
- `.env.example`：环境变量模板
- `account_ids.example.txt`：账号 ID 示例
- `account_names.example.txt`：账号名称映射示例

## 安装

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
```

## 配置

```powershell
Copy-Item .env.example .env
Copy-Item account_ids.example.txt account_ids.txt
Copy-Item account_names.example.txt account_names.txt
```

编辑 `.env`：

```text
DOUYIN_PHONE=your_phone_number
DOUYIN_PASSWORD=your_password
WECHAT_WEBHOOK_URL=https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=your_webhook_key
ENABLE_WECHAT_PUSH=false
MAX_ACCOUNTS=
```

## 运行

```powershell
python popularize.py
```

或：

```powershell
.\run_scripts.bat
```

## 注意

`.env`、`account_ids.txt`、`account_names.txt`、导出文件、日志、截图、`.venv/` 和 `.idea/` 不应提交。后台 DOM 变化后，需要同步调整 Selenium 选择器。
