package io.github.pineconelp.metrics;

import com.sun.management.OperatingSystemMXBean;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import java.lang.management.ManagementFactory;

public class SystemCpuUsageMetric implements MinecraftMetric {
  private ObservableDoubleGauge gauge;

  @Override
  public void register(Meter meter) {
    OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    gauge = meter.gaugeBuilder("system.cpu.utilization")
        .setDescription("The percentage of CPU in use by the whole system")
        .setUnit("1")
        .buildWithCallback(measurement -> {
          measurement.record(osBean.getCpuLoad());
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
