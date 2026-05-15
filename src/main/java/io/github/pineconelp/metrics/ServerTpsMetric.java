package io.github.pineconelp.metrics;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import org.bukkit.plugin.java.JavaPlugin;

public class ServerTpsMetric implements MinecraftMetric {
  private final JavaPlugin plugin;
  private ObservableDoubleGauge gauge;

  public ServerTpsMetric(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  private static final AttributeKey<String> WINDOW_KEY = AttributeKey.stringKey("window");
  private static final Attributes WINDOW_1M = Attributes.of(WINDOW_KEY, "1m");
  private static final Attributes WINDOW_5M = Attributes.of(WINDOW_KEY, "5m");
  private static final Attributes WINDOW_15M = Attributes.of(WINDOW_KEY, "15m");

  @Override
  public void register(Meter meter) {
    gauge = meter.gaugeBuilder("minecraft.server.tps")
        .setDescription("Server ticks per second")
        .setUnit("{tps}")
        .buildWithCallback(measurement -> {
          double[] tps = plugin.getServer().getTPS();
          measurement.record(tps[0], WINDOW_1M);
          measurement.record(tps[1], WINDOW_5M);
          measurement.record(tps[2], WINDOW_15M);
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
