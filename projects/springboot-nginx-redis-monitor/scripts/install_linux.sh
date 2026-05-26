#!/usr/bin/env bash
set -euo pipefail

APP_DIR=/opt/business-service
APP_USER=business
JAR_SOURCE=${1:-target/business-service-0.0.1-SNAPSHOT.jar}

if [[ ! -f "$JAR_SOURCE" ]]; then
  echo "Jar not found: $JAR_SOURCE" >&2
  exit 1
fi

id -u "$APP_USER" >/dev/null 2>&1 || useradd --system --home "$APP_DIR" --shell /usr/sbin/nologin "$APP_USER"

install -d -o "$APP_USER" -g "$APP_USER" "$APP_DIR"
install -d -o "$APP_USER" -g "$APP_USER" /var/log/business-service
install -d /etc/business-service
install -d /etc/prometheus

install -o "$APP_USER" -g "$APP_USER" -m 0644 "$JAR_SOURCE" "$APP_DIR/business-service.jar"
install -m 0644 deploy/systemd/business-service@.service /etc/systemd/system/business-service@.service
install -m 0644 deploy/systemd/env/business-service-1.env.example /etc/business-service/business-service-1.env
install -m 0644 deploy/systemd/env/business-service-2.env.example /etc/business-service/business-service-2.env

install -m 0644 deploy/nginx/business-service.conf /etc/nginx/conf.d/business-service.conf
install -m 0644 deploy/prometheus/prometheus.yml /etc/prometheus/prometheus.yml

systemctl daemon-reload
systemctl enable --now business-service@1 business-service@2
nginx -t
systemctl reload nginx

echo "Installed business-service instances on ports 8081 and 8082."
