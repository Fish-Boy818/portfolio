# 个人技术主页

用于记录系统开发、Java 后端、小程序、自动化工具和模型应用项目。内容以项目结构、实现思路、运行说明和脱敏源码为主。

在线访问：<https://fish-boy818.github.io/portfolio/>

## 项目入口

| 项目 | 说明 |
| --- | --- |
| [`index.html`](index.html) | 在线主页，展示项目概览、技术架构、交互 Demo 和脱敏源码片段 |
| [`projects/account-data-collector/`](projects/account-data-collector/) | 账号管理与数据采集工具 |
| [`projects/miniapp-booking-system/`](projects/miniapp-booking-system/) | 小程序预约与运营后台系统 |
| [`projects/pytorch_sentiment_demo/`](projects/pytorch_sentiment_demo/) | PyTorch 中文情感分类 Demo |

## 技术栈

- Java、Spring Boot、MyBatis、MySQL
- Vue 3、微信小程序、微信登录 / 支付回调
- Python、Selenium、Pandas、Matplotlib、Requests
- PyTorch、TextCNN、FastAPI

## 阅读方式

可以先从 `index.html` 了解整体项目和技术路线，再进入 `projects/` 查看具体源码、运行说明和示例配置。

## 本地预览

```powershell
python -m http.server 8000
```

访问：

```text
http://localhost:8000/
```

## 仓库结构

```text
.
├── index.html
├── assets/
├── projects/
│   ├── account-data-collector/
│   ├── miniapp-booking-system/
│   └── pytorch_sentiment_demo/
├── robots.txt
├── sitemap.xml
├── DEPLOY_GITHUB.md
└── README.md
```

## 公开边界

仓库只保留脱敏后的展示内容和示例配置，不提交真实姓名、账号、密码、Webhook、后台地址、业务数据、导出文件、`.env`、`.venv/` 和 IDE 配置。
