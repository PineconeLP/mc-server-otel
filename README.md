# Minecraft Server OpenTelemetry 

A Paper plugin that tracks and exports telemetry to an OpenTelemetry Collector via OTLP.

## Requirements

- Java 21+
- Paper 1.20.4+
- OpenTelemetry Collector

## Installation

### Download

1. Download the latest jar from [GitHub Releases](https://github.com/PineconeLP/mc-server-otel/releases) 
2. Copy the jar to your server's plugins folder

### Building From Source

1. Build the jar

```sh
./gradlew shadowJar
```

2. Copy the jar from `build/libs/mc-server-otel-*.jar` to your server's plugins folder

## Configuration

See [config.yml](src/main/resources/config.yml).

Reload the config without restarting the server:
 
```
/otel reload
```

> Requires the `otel.reload` permission.

## Usage

### Logs

All server console output is forwarded to the collector as OTLP log records, preserving the original severity (TRACE, DEBUG, INFO, WARN, ERROR, FATAL) and timestamp.

### Metrics

All metrics include a `service.name` resource attribute matching the `service-name` config value.

| Metric | Description | Attributes |
|--------|-------------|------------|
| `minecraft.server.tps` | Server ticks per second | `window`: `1m`, `5m`, `15m` |
| `minecraft.server.mspt` | Average milliseconds per tick | — |
| `minecraft.logs.count` | Number of log event count by severity | `severity` |
| `minecraft.players.online.count` | Number of players currently online | — |
| `minecraft.entities.loaded.count` | Number of loaded entities per world | `world` |
| `minecraft.chunks.loaded.count` | Number of loaded chunks per world | `world` |
| `process.cpu.utilization` | CPU in use by the Minecraft server process (0.0–1.0) | — |
| `system.cpu.utilization` | CPU in use across the whole system (0.0–1.0) | — |
| `process.memory.usage` | JVM heap memory in use by the Minecraft server process | — |
| `system.memory.usage` | Physical RAM in use across the whole system | — |

## Collector Setup

Setup the OTel Collector for use with Prometheus and Loki:

```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

exporters:
  otlphttp:
    endpoint: http://loki:3100/otlp
  prometheus:
    endpoint: 0.0.0.0:8889
    resource_to_telemetry_conversion:
      enabled: true

service:
  pipelines:
    logs:
      receivers: [otlp]
      exporters: [otlphttp]
    metrics:
      receivers: [otlp]
      exporters: [prometheus]
```

Point Prometheus at the collector's metrics endpoint:

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: otel-collector
    static_configs:
      - targets: ["otel-collector:8889"]
```

Manage the OTel collector, Prometheus, Loki, and Grafana via Docker Compose:

```yaml
services:
  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    volumes:
      - grafana_data:/var/lib/grafana
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=changeme
    restart: unless-stopped

  loki:
    image: grafana/loki:latest
    container_name: loki
    ports:
      - "3100:3100"
    volumes:
      - loki_data:/loki
    restart: unless-stopped

  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    restart: unless-stopped

  otel-collector:
    image: otel/opentelemetry-collector-contrib:latest
    container_name: otel-collector
    ports:
      - "4317:4317"
      - "4318:4318"
    volumes:
      - ./otel-collector-config.yaml:/etc/otelcol-contrib/config.yaml
    restart: unless-stopped

volumes:
  grafana_data:
  loki_data:
  prometheus_data:
```
