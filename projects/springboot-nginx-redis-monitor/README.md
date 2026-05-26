# Spring Boot + Nginx + Redis + systemd + Prometheus/Grafana

这是一套可落地的业务服务部署示例：

- Spring Boot 业务服务：`/api/orders/{orderId}`
- Redis 缓存：`@Cacheable` 缓存订单摘要
- Nginx 负载均衡：转发到 `127.0.0.1:8081` 和 `127.0.0.1:8082`
- systemd 托管：使用 `business-service@1`、`business-service@2` 多实例
- Python 巡检脚本：检查 Nginx、后端实例、Redis、Prometheus targets
- Prometheus/Grafana：采集 Actuator Prometheus 指标并预置看板

## 目录

```text
.
├── deploy
│   ├── grafana
│   ├── monitoring
│   ├── nginx
│   ├── prometheus
│   ├── redis
│   └── systemd
├── scripts
│   ├── health_check.py
│   └── install_linux.sh
└── src
```

## 本地构建

需要 JDK 17 和 Maven：

```bash
mvn clean package
```

本机无 Redis 时可以启用 `local` profile，应用会使用内存缓存：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

连接本机 Redis 并启动单实例：

```bash
SERVER_PORT=8081 REDIS_HOST=127.0.0.1 mvn spring-boot:run
```

验证接口：

```bash
curl http://127.0.0.1:8081/api/orders/demo-1001
curl http://127.0.0.1:8081/actuator/health
curl http://127.0.0.1:8081/actuator/prometheus
```

## Linux 部署

建议系统包：

```bash
sudo apt-get update
sudo apt-get install -y openjdk-17-jre-headless maven nginx redis-server prometheus node-exporter
```

构建并安装：

```bash
mvn clean package
sudo bash scripts/install_linux.sh target/business-service-0.0.1-SNAPSHOT.jar
```

`deploy/systemd/env/*.env.example` 是公开示例配置，安装脚本会复制到 `/etc/business-service/*.env`。真实环境可以在服务器上按需调整端口、Redis 地址、日志路径和密码变量，不要把私有 `.env` 文件提交到仓库。

如果要使用示例 Redis 配置：

```bash
sudo install -m 0644 deploy/redis/redis-business.conf /etc/redis/redis-business.conf
sudo systemctl restart redis-server
```

检查服务：

```bash
systemctl status business-service@1
systemctl status business-service@2
systemctl status nginx
```

业务请求会由 Nginx 负载均衡到两个后端实例：

```bash
curl http://127.0.0.1/api/orders/demo-1001
```

## 监控

Prometheus 配置在 `deploy/prometheus/prometheus.yml`，会抓取：

- `127.0.0.1:8081/actuator/prometheus`
- `127.0.0.1:8082/actuator/prometheus`
- `127.0.0.1:9100` node exporter
- `127.0.0.1:9121` redis exporter

Docker Compose 启动 Prometheus、Grafana、redis-exporter、node-exporter。Compose 使用 `deploy/prometheus/prometheus-docker.yml`，通过 `host.docker.internal` 抓取宿主机上的 Spring Boot 实例：

```bash
cd deploy/monitoring
docker compose up -d
```

访问：

- Prometheus: `http://127.0.0.1:9090`
- Grafana: `http://127.0.0.1:3000`，默认账号 `admin`，默认密码来自 `GRAFANA_ADMIN_PASSWORD`，未设置时为本地演示值 `admin`

Grafana 会自动加载 `Business Service Overview` 看板。若部署到可被外部访问的环境，务必设置强密码并限制访问入口。

## 巡检

```bash
python3 scripts/health_check.py
```

自定义地址：

```bash
python3 scripts/health_check.py \
  --service-base-url http://127.0.0.1 \
  --instances http://127.0.0.1:8081,http://127.0.0.1:8082 \
  --redis-host 127.0.0.1 \
  --prometheus-url http://127.0.0.1:9090
```

脚本输出 JSON，所有检查通过时退出码为 `0`，否则退出码为 `2`，适合接入 cron、CI 或监控告警。

## 关键文件

- Spring Boot 配置：[src/main/resources/application.yml](src/main/resources/application.yml)
- Nginx 配置：[deploy/nginx/business-service.conf](deploy/nginx/business-service.conf)
- systemd 模板：[deploy/systemd/business-service@.service](deploy/systemd/business-service@.service)
- systemd 示例环境文件：[deploy/systemd/env/](deploy/systemd/env/)
- Prometheus 配置：[deploy/prometheus/prometheus.yml](deploy/prometheus/prometheus.yml)
- Grafana 看板：[deploy/grafana/provisioning/dashboards/business-service-overview.json](deploy/grafana/provisioning/dashboards/business-service-overview.json)
- 巡检脚本：[scripts/health_check.py](scripts/health_check.py)

## 公开边界

仓库保留源码、部署模板、监控配置和示例环境文件；未提交本机 JDK、构建产物、运行日志、真实服务器配置和私有环境变量。
