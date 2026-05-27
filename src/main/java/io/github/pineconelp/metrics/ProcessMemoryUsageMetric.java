package io.github.pineconelp.metrics;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;

public class ProcessMemoryUsageMetric implements MinecraftMetric {
  private ObservableLongGauge gauge;

  @Override
  public void register(Meter meter) {
    gauge = meter.gaugeBuilder("process.memory.usage")
        .setDescription("The amount of JVM heap memory in use by the Minecraft server process")
        .setUnit("By")
        .ofLongs()
        .buildWithCallback(measurement -> {
          Runtime runtime = Runtime.getRuntime();
          measurement.record(runtime.totalMemory() - runtime.freeMemory());
        });
  }

  @Override
  public void close() {
    if (gauge != null) {
      gauge.close();
      gauge = null;
    }
  }
}
