package io.github.pineconelp.metrics;

import com.sun.management.OperatingSystemMXBean;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import java.lang.management.ManagementFactory;

public class SystemMemoryUsageMetric implements MinecraftMetric {
  private ObservableLongGauge gauge;

  @Override
  public void register(Meter meter) {
    OperatingSystemMXBean osBean =
        (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    gauge = meter.gaugeBuilder("system.memory.usage")
        .setDescription("The amount of physical RAM in use across the whole system")
        .setUnit("By")
        .ofLongs()
        .buildWithCallback(measurement -> {
          measurement.record(osBean.getTotalMemorySize() - osBean.getFreeMemorySize());
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
