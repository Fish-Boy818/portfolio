# 王月作品集

系统开发工程师 / Java 后端开发方向在线作品集。

在线访问：<https://fish-boy818.github.io/portfolio/>

## 内容亮点

- 个人概览：学历、方向、经历脉络和系统开发定位。
- 岗位能力：后端开发、数据库设计、前后端集成和交付协作。
- 核心能力：接口设计、业务建模、状态流转、数据输出和工程拆解。
- 代表项目：活动推广与后台运营系统、自动化采集与数据分析工具、PyTorch 文本分类 Demo。
- 技术架构：Client / Admin / API / Service / Data 分层与模块覆盖。
- 交互 Demo：静态后台控制台，模拟活动管理、订单履约、用户增长和系统设置。
- 脱敏源码片段：Controller、Service、Python Pipeline、SQL Schema 和 PyTorch 示例。

## 技术栈

- Java 8、Spring Boot、Spring MVC、MyBatis、Maven
- MySQL、SQL、分页查询、初始化脚本、事务处理
- Vue 3、微信小程序、微信登录、微信支付回调
- Python、Selenium、Pandas、NumPy、Matplotlib、OpenPyXL、Requests
- PyTorch、TextCNN、字符级编码、模型推理封装

## 本地预览

这个仓库是纯静态站点，不需要安装依赖。可以直接打开 `index.html`，也可以启动本地静态服务：

```powershell
python -m http.server 8000
```

然后访问：

```text
http://localhost:8000/
```

## 发布方式

仓库已适配 GitHub Pages：

- `.nojekyll`：禁用 Jekyll 处理，按静态文件原样发布。
- `robots.txt`：允许搜索引擎抓取公开页面。
- `sitemap.xml`：提供站点地图，便于搜索引擎发现页面。
- `index.html`：包含 SEO、Open Graph、Twitter Card 和结构化数据元信息。
- `projects/pytorch_sentiment_demo/`：PyTorch 中文情感分类 Demo 源码、样例数据和测试。
- `projects/douyin-laike-account-tool/`：抖音来客账号管理与数据采集工具，已移除真实账号和凭据。

GitHub Pages 建议配置：

- Source：`Deploy from a branch`
- Branch：`main`
- Folder：`/ (root)`

## 项目边界

项目涉及已交付业务系统，源码、后台地址和真实数据不对外公开。公开页面保留系统结构、实现方式、交付流程和脱敏后的展示素材。

已处理信息：

- 手机号、邮箱等联系方式
- 客户 / 项目真实名称
- 本机路径与环境信息
- 账号数据和业务报表明细
- AppID、商户号、Webhook、证书路径等关键配置

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

## 维护建议

- 新增项目时同步更新首页内容、README、`sitemap.xml` 的 `lastmod`。
- 新增图片时优先使用压缩后的 PNG/JPG/WebP，并检查移动端显示。
- 发布前检查页面中不要出现真实手机号、邮箱、后台地址、密钥、证书路径和客户数据。
