# 抖音来客账号管理与数据采集工具

这个项目用于维护抖音来客账号列表，并通过 Selenium 自动登录后台、采集账号数据、生成报表图片，可选推送到企业微信机器人。

公开仓库版本已经移除真实账号、登录凭据、Webhook 和业务数据，只保留可复用的工程结构和示例配置。

## 项目能力

| 模块 | 说明 |
| --- | --- |
| 登录自动化 | 使用 Selenium 打开抖音来客后台并完成登录流程 |
| 账号维护 | 通过账号 ID 文件和名称映射文件维护采集范围 |
| 数据采集 | 切换多账号，采集运营数据并做结构化处理 |
| 报表生成 | 使用 Pandas / Matplotlib / Pillow 生成图片或表格输出 |
| 消息推送 | 可选将结果发送到企业微信机器人，默认关闭 |

## 文件说明

```text
.
├── popularize.py              # 主采集脚本
├── account_manager.py         # 账号管理与提取逻辑
├── requirements.txt           # Python 依赖
├── .env.example               # 环境变量模板
├── account_ids.example.txt    # 账号 ID 示例
├── account_names.example.txt  # 账号名称映射示例
├── run_scripts.bat            # Windows 快捷运行脚本
└── README.md
```

## 安装

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
```

## 配置

复制示例文件：

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

配置说明：

| 变量 | 说明 |
| --- | --- |
| `DOUYIN_PHONE` | 抖音来客登录手机号 |
| `DOUYIN_PASSWORD` | 抖音来客登录密码 |
| `WECHAT_WEBHOOK_URL` | 企业微信机器人地址，可为空 |
| `ENABLE_WECHAT_PUSH` | 是否启用推送，建议默认 `false` |
| `MAX_ACCOUNTS` | 最大处理账号数，留空表示使用脚本默认值 |

账号文件格式：

```text
# account_ids.txt
1234567890123
2345678901234
```

```text
# account_names.txt
1234567890123:示例账号A
2345678901234:示例账号B
```

## 运行

```powershell
python popularize.py
```

或：

```powershell
.\run_scripts.bat
```

## 安全边界

以下内容必须只保留在本地，不能提交：

- `.env`
- `account_ids.txt`
- `account_names.txt`
- 登录手机号、密码、Webhook key
- 导出的图片、Excel、截图、日志
- `.venv/`、`.idea/`、`__pycache__/`
- 本地历史副本和测试草稿

如果真实密码或 Webhook 曾经写入过代码，建议立即更换密码并重置 Webhook key。

## 维护建议

- 页面选择器依赖抖音来客后台 DOM，后台改版后需要优先检查 Selenium selector。
- 对外展示时只保留示例账号，不要提交真实商户名称或账号 ID。
- 新增输出文件类型时同步更新 `.gitignore`，避免误传业务数据。
