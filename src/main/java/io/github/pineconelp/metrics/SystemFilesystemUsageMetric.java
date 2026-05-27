package io.github.pineconelp.metrics;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import java.io.File;

public class SystemFilesystemUsageMetric implements MinecraftMetric {
  private ObservableLongGauge gauge;

  private static final AttributeKey<String> MOUNTPOINT_KEY = AttributeKey.stringKey("system.filesystem.mountpoint");
  private static final AttributeKey<String> STATE_KEY = AttributeKey.stringKey("system.filesystem.state");

  @Override
  public void register(Meter meter) {
    gauge = meter.gaugeBuilder("system.filesystem.usage")
        .setDescription("Disk space used and free per filesystem mount point")
        .setUnit("By")
        .ofLongs()
        .buildWithCallback(measurement -> {
          for (File root : File.listRoots()) {
            String mountpoint = root.getAbsolutePath();
            measurement.record(
                root.getTotalSpace() - root.getUsableSpace(),
                Attributes.of(MOUNTPOINT_KEY, mountpoint, STATE_KEY, "used"));
            measurement.record(
                root.getUsableSpace(),
                Attributes.of(MOUNTPOINT_KEY, mountpoint, STATE_KEY, "free"));
          }
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
