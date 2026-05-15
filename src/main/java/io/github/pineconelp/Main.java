package io.github.pineconelp;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.github.pineconelp.logs.ConsoleLogOtelBridge;
import io.opentelemetry.api.metrics.Meter;
import io.github.pineconelp.metrics.MinecraftMetric;
import io.github.pineconelp.metrics.ChunksLoadedMetric;
import io.github.pineconelp.metrics.EntitiesLoadedMetric;
import io.github.pineconelp.metrics.PlayersOnlineMetric;
import io.github.pineconelp.metrics.ServerTpsMetric;
import org.apache.logging.log4j.LogManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main extends JavaPlugin {
  private final List<MinecraftMetric> activeMetrics;

  private OpenTelemetrySdk otelSdk;
  private ConsoleLogOtelBridge appender;

  public Main() {
    super();

    activeMetrics = new ArrayList<>();
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();
    startOtel();
  }

  @Override
  public void onDisable() {
    activeMetrics.forEach(MinecraftMetric::close);
    activeMetrics.clear();

    if (appender != null) {
      getServerConsoleLogger().removeAppender(appender);
      appender.stop();
    }

    if (otelSdk != null) {
      otelSdk.close();
    }
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!command.getName().equalsIgnoreCase("otel")) {
      return false;
    }

    if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
      if (!sender.hasPermission("otel.reload")) {
        sender.sendMessage("You do not have permission to use this command.");
        return true;
      }

      onDisable();
      reloadConfig();
      startOtel();

      sender.sendMessage("Reloaded McServerOtel.");

      return true;
    }

    sender.sendMessage("Usage: /otel reload");

    return true;
  }

  private void startOtel() {
    String serviceName = getConfig().getString("service-name", "minecraft-server");
    String logsEndpoint = getConfig().getString("logs.otlp-endpoint", "http://localhost:4318/v1/logs");
    String metricsEndpoint = getConfig().getString("metrics.otlp-endpoint", "http://localhost:4318/v1/metrics");
    int metricsInterval = getConfig().getInt("metrics.export-interval-seconds", 60);

    Resource resource = Resource.getDefault().toBuilder()
        .put(AttributeKey.stringKey("service.name"), serviceName)
        .build();

    otelSdk = OpenTelemetrySdk.builder()
        .setLoggerProvider(SdkLoggerProvider.builder()
            .setResource(resource)
            .addLogRecordProcessor(BatchLogRecordProcessor.builder(
                OtlpHttpLogRecordExporter.builder()
                    .setEndpoint(logsEndpoint)
                    .build())
                .build())
            .build())
        .setMeterProvider(SdkMeterProvider.builder()
            .setResource(resource)
            .registerMetricReader(PeriodicMetricReader.builder(
                OtlpHttpMetricExporter.builder()
                    .setEndpoint(metricsEndpoint)
                    .build())
                .setInterval(metricsInterval, TimeUnit.SECONDS)
                .build())
            .build())
        .build();

    Logger otelLogger = otelSdk.getLogsBridge().get("console-otel-bridge");
    appender = new ConsoleLogOtelBridge(otelLogger);
    appender.start();
    getServerConsoleLogger().addAppender(appender);

    Meter meter = otelSdk.getMeter("mc-server-otel");
    registerMetricIfEnabled("metrics.types.player-count", new PlayersOnlineMetric(this), meter);
    registerMetricIfEnabled("metrics.types.tps", new ServerTpsMetric(this), meter);
    registerMetricIfEnabled("metrics.types.entity-count", new EntitiesLoadedMetric(this), meter);
    registerMetricIfEnabled("metrics.types.loaded-chunks", new ChunksLoadedMetric(this), meter);
  }

  private void registerMetricIfEnabled(String configKey, MinecraftMetric metric, Meter meter) {
    if (getConfig().getBoolean(configKey, true)) {
      metric.register(meter);
      activeMetrics.add(metric);
    }
  }

  private static org.apache.logging.log4j.core.Logger getServerConsoleLogger() {
    return (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
  }
}
