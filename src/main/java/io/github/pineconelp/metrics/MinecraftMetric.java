package io.github.pineconelp.metrics;

import io.opentelemetry.api.metrics.Meter;

public interface MinecraftMetric {
  void register(Meter meter);
  void close();
}
