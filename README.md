# 王月作品集

系统开发工程师 / Java 后端开发方向在线作品集，重点展示业务系统开发、后端接口设计、数据处理自动化和技术项目协同能力。

在线访问：<https://fish-boy818.github.io/portfolio/>

## 快速导航

| 内容 | 入口 | 适合查看 |
| --- | --- | --- |
| 在线作品集页面 | [`index.html`](index.html) | 个人概览、项目展示、技术架构、交互 Demo |
| 抖音来客账号工具 | [`projects/douyin-laike-account-tool/`](projects/douyin-laike-account-tool/) | Selenium 自动化、账号管理、数据采集、报表推送 |
| PyTorch 情感分类 Demo | [`projects/pytorch_sentiment_demo/`](projects/pytorch_sentiment_demo/) | PyTorch 入门项目、训练流程、推理接口 |
| 部署说明 | [`DEPLOY_GITHUB.md`](DEPLOY_GITHUB.md) | GitHub Pages 发布配置 |

## 仓库亮点

- 作品集页面：以静态单页方式展示系统开发经历、项目拆解、技术架构和脱敏源码片段。
- 业务自动化项目：通过 Selenium、Pandas、Matplotlib 串联登录、采集、清洗、报表生成和企业微信推送。
- 机器学习 Demo：覆盖 Dataset、DataLoader、TextCNN/MLP、训练保存、命令行推理和 FastAPI HTTP 推理。
- 公开安全处理：真实账号、登录凭据、Webhook、业务数据、导出文件和本地环境均已从可提交内容中排除。

## 技术栈

| 方向 | 技术 |
| --- | --- |
| Java 后端 | Java 8、Spring Boot、Spring MVC、MyBatis、Maven |
| 数据库 | MySQL、SQL、分页查询、初始化脚本、事务处理 |
| 前端与小程序 | Vue 3、微信小程序、微信登录、微信支付回调 |
| 自动化与数据 | Python、Selenium、Pandas、NumPy、Matplotlib、OpenPyXL、Requests |
| 机器学习 | PyTorch、TextCNN、字符级编码、模型推理封装 |

## 本地预览作品集

这个仓库首页是纯静态站点，不需要安装依赖。可以直接打开 `index.html`，也可以启动本地静态服务：

```powershell
python -m http.server 8000
```

访问：

```text
http://localhost:8000/
```

## 子项目说明

### douyin-laike-account-tool

抖音来客账号管理与数据采集工具。公开版本保留工程结构、配置方式和核心自动化逻辑，但不包含真实账号、密码、Webhook 和业务数据。

主要内容：

- `popularize.py`：Selenium 自动化采集入口。
- `account_manager.py`：账号 ID 与名称维护逻辑。
- `.env.example`：登录与推送配置模板。
- `account_ids.example.txt` / `account_names.example.txt`：账号配置示例。

### pytorch_sentiment_demo

中文短文本情感分类练习项目。适合展示 PyTorch 从数据集构建到模型训练、保存、加载和接口部署的完整链路。

主要内容：

- `src/text_dataset.py`：文本清洗、字符级分词、词表构建和编码。
- `src/models.py`：MLP 与 TextCNN 分类模型。
- `src/train.py`：训练、评估和模型保存。
- `src/infer.py` / `src/api.py`：命令行推理与 HTTP 推理接口。

## 仓库结构

```text
.
├── index.html
├── assets/
├── projects/
│   ├── douyin-laike-account-tool/
│   └── pytorch_sentiment_demo/
├── robots.txt
├── sitemap.xml
├── .nojekyll
├── DEPLOY_GITHUB.md
└── README.md
```

## GitHub Pages

仓库已适配 GitHub Pages：

- `.nojekyll`：禁用 Jekyll 处理，按静态文件原样发布。
- `robots.txt`：允许搜索引擎抓取公开页面。
- `sitemap.xml`：提供站点地图。
- `index.html`：包含 SEO、Open Graph、Twitter Card 和结构化数据元信息。

推荐 Pages 配置：

- Source：`Deploy from a branch`
- Branch：`main`
- Folder：`/ (root)`

## 公开边界

项目涉及已交付业务系统和个人项目，源码、后台地址和真实数据不对外公开。公开仓库只保留系统结构、实现方式、交付流程和脱敏后的展示素材。

提交前需要确认不包含：

- 手机号、邮箱、账号、密码、Token、Webhook、证书路径
- 客户 / 项目真实名称和后台地址
- 账号数据、业务报表明细、导出文件和截图
- `.env`、`.venv/`、`.idea/`、缓存和日志

## 维护建议

- 新增项目时同步更新首页内容、README、`sitemap.xml` 的 `lastmod`。
- 新增图片时优先使用压缩后的 PNG/JPG/WebP，并检查移动端显示。
- 发布前运行敏感词搜索，确认没有真实凭据或业务数据进入提交。
