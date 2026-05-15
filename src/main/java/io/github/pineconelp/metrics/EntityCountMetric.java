package io.github.pineconelp.metrics;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import org.bukkit.plugin.java.JavaPlugin;

public class EntityCountMetric implements MinecraftMetric {
  private final JavaPlugin plugin;
  private ObservableDoubleGauge gauge;

  public EntityCountMetric(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void register(Meter meter) {
    AttributeKey<String> worldKey = AttributeKey.stringKey("world");
    gauge = meter.gaugeBuilder("minecraft.entities")
        .setDescription("Number of entities per world")
        .setUnit("{entities}")
        .buildWithCallback(measurement ->
            plugin.getServer().getWorlds().forEach(world ->
                measurement.record(
                    world.getEntityCount(),
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
