#!/usr/bin/env python3
import argparse
import json
import socket
import sys
import time
import urllib.error
import urllib.request


def http_get(url, timeout):
    start = time.time()
    try:
        with urllib.request.urlopen(url, timeout=timeout) as response:
            body = response.read(4096).decode("utf-8", errors="replace")
            return {
                "ok": 200 <= response.status < 300,
                "status": response.status,
                "latency_ms": round((time.time() - start) * 1000, 2),
                "body": body,
            }
    except urllib.error.HTTPError as exc:
        return {
            "ok": False,
            "status": exc.code,
            "latency_ms": round((time.time() - start) * 1000, 2),
            "error": str(exc),
        }
    except Exception as exc:
        return {
            "ok": False,
            "status": None,
            "latency_ms": round((time.time() - start) * 1000, 2),
            "error": str(exc),
        }


def tcp_check(host, port, timeout):
    start = time.time()
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return {
                "ok": True,
                "latency_ms": round((time.time() - start) * 1000, 2),
            }
    except Exception as exc:
        return {
            "ok": False,
            "latency_ms": round((time.time() - start) * 1000, 2),
            "error": str(exc),
        }


def redis_ping(host, port, timeout):
    start = time.time()
    try:
        with socket.create_connection((host, port), timeout=timeout) as sock:
            sock.settimeout(timeout)
            sock.sendall(b"*1\r\n$4\r\nPING\r\n")
            data = sock.recv(16)
            return {
                "ok": data.startswith(b"+PONG"),
                "latency_ms": round((time.time() - start) * 1000, 2),
                "response": data.decode("utf-8", errors="replace").strip(),
            }
    except Exception as exc:
        return {
            "ok": False,
            "latency_ms": round((time.time() - start) * 1000, 2),
            "error": str(exc),
        }


def prometheus_targets(base_url, timeout):
    result = http_get(f"{base_url.rstrip('/')}/api/v1/targets", timeout)
    if not result["ok"]:
        return result
    try:
        payload = json.loads(result["body"])
        active_targets = payload.get("data", {}).get("activeTargets", [])
        unhealthy = [
            {
                "job": target.get("labels", {}).get("job"),
                "instance": target.get("labels", {}).get("instance"),
                "health": target.get("health"),
                "lastError": target.get("lastError"),
            }
            for target in active_targets
            if target.get("health") != "up"
        ]
        return {
            "ok": len(unhealthy) == 0,
            "active_targets": len(active_targets),
            "unhealthy": unhealthy,
            "latency_ms": result["latency_ms"],
        }
    except Exception as exc:
        return {
            "ok": False,
            "latency_ms": result["latency_ms"],
            "error": f"Failed to parse Prometheus response: {exc}",
        }


def main():
    parser = argparse.ArgumentParser(description="Business service health inspection")
    parser.add_argument("--nginx-url", default="http://127.0.0.1/nginx-health")
    parser.add_argument("--service-base-url", default="http://127.0.0.1")
    parser.add_argument("--instances", default="http://127.0.0.1:8081,http://127.0.0.1:8082")
    parser.add_argument("--redis-host", default="127.0.0.1")
    parser.add_argument("--redis-port", default=6379, type=int)
    parser.add_argument("--prometheus-url", default="http://127.0.0.1:9090")
    parser.add_argument("--timeout", default=3.0, type=float)
    args = parser.parse_args()

    checks = {
        "nginx": http_get(args.nginx_url, args.timeout),
        "business_route": http_get(f"{args.service_base_url.rstrip('/')}/api/orders/demo-1001", args.timeout),
        "redis": redis_ping(args.redis_host, args.redis_port, args.timeout),
        "prometheus": prometheus_targets(args.prometheus_url, args.timeout),
        "instances": {},
    }

    for instance in [item.strip() for item in args.instances.split(",") if item.strip()]:
        checks["instances"][instance] = http_get(f"{instance.rstrip('/')}/actuator/health/readiness", args.timeout)

    ok = all([
        checks["nginx"]["ok"],
        checks["business_route"]["ok"],
        checks["redis"]["ok"],
        checks["prometheus"]["ok"],
        all(item["ok"] for item in checks["instances"].values()),
    ])

    output = {
        "ok": ok,
        "checks": checks,
    }
    print(json.dumps(output, ensure_ascii=False, indent=2))
    return 0 if ok else 2


if __name__ == "__main__":
    sys.exit(main())
