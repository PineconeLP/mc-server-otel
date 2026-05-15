package io.github.pineconelp.metrics;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import org.bukkit.plugin.java.JavaPlugin;

public class LoadedChunkMetric implements MinecraftMetric {
  private final JavaPlugin plugin;
  private ObservableDoubleGauge gauge;

  public LoadedChunkMetric(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void register(Meter meter) {
    AttributeKey<String> worldKey = AttributeKey.stringKey("world");
    gauge = meter.gaugeBuilder("minecraft.chunks.loaded")
        .setDescription("Number of loaded chunks per world")
        .setUnit("{chunks}")
        .buildWithCallback(measurement ->
            plugin.getServer().getWorlds().forEach(world ->
                measurement.record(
                    world.getLoadedChunks().length,
                    Attributes.of(worldKey, world.getName()))));
  }

  @Override
  public void close() {
    if (gauge != null) {
      gauge.close();
      gauge = null;
    }
  }
}
