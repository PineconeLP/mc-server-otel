package io.github.pineconelp.metrics;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import org.bukkit.plugin.java.JavaPlugin;

public class ServerTpsMetric implements MinecraftMetric {
  private final JavaPlugin plugin;
  private ObservableDoubleGauge gauge;

  public ServerTpsMetric(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void register(Meter meter) {
    gauge = meter.gaugeBuilder("minecraft.server.tps")
        .setDescription("Server ticks per second")
        .setUnit("{tps}")
        .buildWithCallback(measurement -> measurement.record(plugin.getServer().getTPS()[0]));
  }

  @Override
  public void close() {
    if (gauge != null) {
      gauge.close();
      gauge = null;
    }
  }
}
