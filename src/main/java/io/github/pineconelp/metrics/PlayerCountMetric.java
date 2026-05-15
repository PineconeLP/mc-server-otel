package io.github.pineconelp.metrics;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerCountMetric implements MinecraftMetric {
  private final JavaPlugin plugin;
  private ObservableDoubleGauge gauge;

  public PlayerCountMetric(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void register(Meter meter) {
    gauge = meter.gaugeBuilder("minecraft.players.online")
        .setDescription("Number of players currently online")
        .setUnit("{players}")
        .buildWithCallback(measurement ->
            measurement.record(plugin.getServer().getOnlinePlayers().size()));
  }

  @Override
  public void close() {
    if (gauge != null) {
      gauge.close();
      gauge = null;
    }
  }
}
