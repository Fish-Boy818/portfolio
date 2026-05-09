# 王月作品集 Demo

这个目录已根据脱敏后的简历信息优化，定位为系统开发工程师 / Java 后端开发 / 信息系统工程师作品集。

内容由两个本地项目生成：

- `<Python 自动化项目路径>`：外部平台账号自动化采集、Excel/PNG 报表、内部群机器人推送。
- `<Java 业务系统项目路径>`：微信小程序 + Spring Boot 后端 + 管理端后台。

## 打开方式

给 HR 查看时，请发送整个压缩包或完整文件夹。不要只发送 `index.html`，因为页面图片依赖 `assets` 目录。

直接用浏览器打开：

```powershell
<作品集目录>\index.html
```

或者在 PowerShell 中执行：

```powershell
Start-Process '<作品集目录>\index.html'
```

## 内容说明

- `index.html`：单文件静态作品集页面，包含岗位匹配、项目经历、交互式后台缩略 demo、业务流程、运行入口。
- `assets/python`：复制自 Python 项目已生成的报表图片，已进行模糊和遮罩脱敏。
- `assets/miniapp`：复制自微信小程序项目的活动、俱乐部和图标素材。

## 本次优化重点

- 首页从“项目展示”改成“王月的工程作品集”，直接匹配系统开发 / Java 后端岗位。
- 增加核心能力区，将后端接口、数据库设计、集成对接、自动化数据能力映射到项目产物。
- 项目顺序调整为 Java/Spring Boot 业务系统优先，Python 自动化工具作为第二项目补充。
- 增加岗位匹配区，覆盖后端业务开发、数据库与性能、前后端集成、交付协作。
- 增加技术架构区，展示 Client/Admin/API/Service/Data 分层、核心模块、技术栈矩阵和工程产物。
- 项目描述按简历 STAR 逻辑改写，突出需求对接、接口设计、表结构设计、支付回调、联调交付。
- 保留脱敏后的项目截图和素材，避免空泛包装。

## 敏感信息处理

扫描时发现简历和原项目中包含手机号、邮箱、客户/项目名、本机路径、账号数据、业务报表、AppID、商户号、Webhook、证书路径等敏感配置。作品集页面只展示求职方向、技术能力和脱敏后的运行方式，没有写入完整联系方式、完整路径或完整敏感配置。

## 原项目运行入口

以下命令仅用于说明源码项目在本地具备启动方式，HR 查看作品集时不需要执行。

Python 项目：

```powershell
Set-Location -LiteralPath '<Python 自动化项目路径>'
python account_manager.py list
.\run_scripts.bat
```

Java/小程序项目：

```powershell
Set-Location -LiteralPath '<Java 业务系统项目路径>'
mvn spring-boot:run
```

管理端默认入口：

```text
http://localhost:8080/admin
```

小程序需要用微信开发者工具导入：

```text
<Java 业务系统项目路径>\miniprogram
```
