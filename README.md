# 个人技术主页

用于记录系统开发、Java 后端、小程序、自动化工具和模型应用相关实践。仓库内容以公开主页、项目结构、实现思路、运行说明和脱敏源码为主。

[在线访问](https://fish-boy818.github.io/portfolio/) · [GitHub 仓库](https://github.com/Fish-Boy818/portfolio)

## 快速入口

| 入口 | 说明 |
| --- | --- |
| [`index.html`](index.html) | 个人技术主页，展示项目概览、技术架构、交互 Demo 和源码片段 |
| [`projects/miniapp-booking-system/`](projects/miniapp-booking-system/) | 小程序预约与运营后台系统公开版 |
| [`projects/account-data-collector/`](projects/account-data-collector/) | 账号管理与数据采集工具公开版 |
| [`projects/pytorch_sentiment_demo/`](projects/pytorch_sentiment_demo/) | PyTorch 中文情感分类 Demo |
| [`projects/springboot-nginx-redis-monitor/`](projects/springboot-nginx-redis-monitor/) | Spring Boot 多实例部署与监控示例 |

## 内容地图

这个仓库主要展示五类内容：

- **系统开发**：Spring Boot、MyBatis、MySQL、RESTful API、业务状态流转。
- **小程序与后台**：微信小程序、Vue 3 管理后台、登录、支付回调、文件上传。
- **自动化工具**：Python、Selenium、Pandas、图表生成、Excel 输出。
- **模型应用**：PyTorch、TextCNN、FastAPI 推理接口。
- **部署与监控**：Nginx 负载均衡、Redis 缓存、systemd 多实例、Prometheus/Grafana。

## 代表作品

### 小程序预约与运营后台系统

Spring Boot 后端、微信小程序端和静态管理后台示例项目。公开版本保留系统结构、接口分层、数据表设计和运行说明，移除真实 AppID、商户号、域名、证书路径、上传文件和部署脚本。

适合查看：

- Controller / Service / Mapper 分层
- MySQL `schema.sql` 与演示初始化数据
- 微信登录、支付参数、支付回调相关服务
- 小程序页面结构与后台管理页面

### 账号管理与数据采集工具

Python 自动化数据工具公开版，展示从页面采集、字段清洗、统计汇总到图表和 Excel 输出的流程。真实登录地址、账号、密码、Webhook、账号列表和导出结果均未提交。

适合查看：

- Selenium 自动化流程
- Pandas 数据清洗和统计
- 图片报表与 Excel 导出
- 环境变量配置方式

### PyTorch 中文情感分类 Demo

中文文本情感分类示例，包含训练数据、模型训练、推理逻辑和 FastAPI 服务封装。

适合查看：

- TextCNN 模型结构
- PyTorch 训练与推理流程
- FastAPI 推理接口
- 小型机器学习 Demo 的工程组织

### Spring Boot 多实例部署与监控示例

围绕一个订单查询服务，整理从本地运行到 Linux 部署的完整链路：Spring Boot 3、Redis 缓存、Nginx 负载均衡、systemd 多实例托管、Prometheus/Grafana 监控和 Python 巡检脚本。

适合查看：

- Actuator health、metrics、prometheus 监控端点
- Nginx upstream 与 systemd 服务模板
- Redis 缓存配置和 exporter 监控配置
- Python 健康检查脚本与 Docker Compose 监控环境

## 技术栈

```text
Backend      Java, Spring Boot, Spring MVC, MyBatis, MySQL, Maven
Frontend     Vue 3, HTML, CSS, JavaScript, WeChat Mini Program
Automation   Python, Selenium, Pandas, Matplotlib, Requests
ML Demo      PyTorch, TextCNN, FastAPI
DevOps       Nginx, Redis, systemd, Prometheus, Grafana, Docker Compose
Workflow     Git, GitHub Pages, Markdown, RESTful API
```

## 本地预览

仓库主页是静态页面，可以直接用本地 HTTP 服务预览：

```powershell
python -m http.server 8000
```

访问：

```text
http://localhost:8000/
```

后端、小程序和 Python 项目的运行方式见各项目目录下的 README。

## 仓库结构

```text
.
├── index.html
├── assets/
├── projects/
│   ├── account-data-collector/
│   ├── miniapp-booking-system/
│   ├── pytorch_sentiment_demo/
│   └── springboot-nginx-redis-monitor/
├── robots.txt
├── sitemap.xml
├── DEPLOY_GITHUB.md
└── README.md
```

