package io.github.pineconelp.metrics;

import com.sun.management.OperatingSystemMXBean;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import java.lang.management.ManagementFactory;

public class ProcessCpuUsageMetric implements MinecraftMetric {
  private ObservableDoubleGauge gauge;

  @Override
  public void register(Meter meter) {
    OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    gauge = meter.gaugeBuilder("process.cpu.utilization")
        .setDescription("The percentage of CPU in use by the Minecraft server process")
        .setUnit("1")
        .buildWithCallback(measurement -> {
          measurement.record(osBean.getProcessCpuLoad());
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
