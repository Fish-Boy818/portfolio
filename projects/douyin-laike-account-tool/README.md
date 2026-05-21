# 抖音来客数据采集与账号管理工具

这个项目用于维护抖音来客账号列表，并通过 Selenium 自动登录后台、采集账号数据、生成报表图片，可选推送到企业微信机器人。

## 功能

- 账号 ID 与账号名称的本地维护
- Selenium 自动登录抖音来客后台
- 多账号数据采集与处理
- Matplotlib / Pandas 报表生成
- 企业微信机器人推送，默认关闭

## 公开仓库边界

真实登录信息、企业微信 webhook、账号 ID、账号名称和导出的业务数据不应提交到 Git。仓库中只保留示例配置：

- `.env.example`
- `account_ids.example.txt`
- `account_names.example.txt`

本地运行时复制一份并填入真实值：

```powershell
Copy-Item .env.example .env
Copy-Item account_ids.example.txt account_ids.txt
Copy-Item account_names.example.txt account_names.txt
```

## 安装依赖

建议使用虚拟环境：

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
```

## 配置环境变量

编辑 `.env`：

```text
DOUYIN_PHONE=your_phone_number
DOUYIN_PASSWORD=your_password
WECHAT_WEBHOOK_URL=https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=your_webhook_key
ENABLE_WECHAT_PUSH=false
MAX_ACCOUNTS=
```

`ENABLE_WECHAT_PUSH` 默认建议保持 `false`。需要推送时再改为 `true`，并确认 webhook 有效。

## 账号文件

`account_ids.txt` 每行一个账号 ID：

```text
1234567890123
2345678901234
```

`account_names.txt` 使用 `ID:名称` 格式：

```text
1234567890123:示例账号A
2345678901234:示例账号B
```

## 账号管理

```powershell
python account_manager.py
```

也可以按实际需要扩展命令行入口，用于添加、删除或重命名账号。

## 运行采集

```powershell
python popularize.py
```

或运行：

```powershell
.\run_scripts.bat
```

## 上传前检查

提交前确认以下文件不会进入 Git：

- `.env`
- `.venv/`
- `.idea/`
- `account_ids.txt`
- `account_names.txt`
- 导出的图片、Excel、日志和截图
- 本地历史副本 `popularize copy*.py`
- 本地测试草稿 `test(1).py`

如果曾经把真实密码或 webhook 写入代码，建议立即更换密码并重置 webhook key。

