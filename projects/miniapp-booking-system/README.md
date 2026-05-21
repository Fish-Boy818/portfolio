# 小程序预约与运营后台系统

Spring Boot 后端、微信小程序端和静态管理后台示例项目。公开版本已移除真实 AppID、商户号、域名、证书路径、上传文件和部署脚本。

## 内容

- `src/main/java/`：后端接口、服务、模型和 MyBatis Mapper
- `src/main/resources/schema.sql`：数据库结构
- `src/main/resources/data.sql`：演示初始化数据
- `src/main/resources/static/admin/`：后台管理页面
- `miniprogram/`：微信小程序端代码

## 后端运行

准备 MySQL 数据库后，按需设置环境变量：

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/miniapp_booking?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
$env:WECHAT_APPID="your_appid"
$env:WECHAT_SECRET="your_secret"
```

启动：

```powershell
mvn spring-boot:run
```

打包：

```powershell
mvn -DskipTests package
```

## 小程序运行

用微信开发者工具打开 `miniprogram/`。公开版本使用 `touristappid`，真实 AppID 请在本地替换，不要提交。

接口地址在 `miniprogram/utils/api.js` 中配置：

```js
const BASE_URLS = {
  local: "http://127.0.0.1:8080",
  cloud: "https://example.com"
};
```

## 注意

不要提交真实 `.env`、`uploads/`、`target/`、部署脚本、服务器地址、商户号、证书路径、生产域名和业务数据。
