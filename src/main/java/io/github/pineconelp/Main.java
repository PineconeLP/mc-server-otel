package io.github.pineconelp;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import org.apache.logging.log4j.LogManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
  private OpenTelemetrySdk otelSdk;
  private ConsoleLogOtelBridge appender;
  private ObservableDoubleGauge playerCountGauge;
  private ObservableDoubleGauge tpsGauge;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    startOtelBridge();
  }

  @Override
  public void onDisable() {
    if (playerCountGauge != null) {
      playerCountGauge.close();
      playerCountGauge = null;
    }

    if (tpsGauge != null) {
      tpsGauge.close();
      tpsGauge = null;
    }

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
      startOtelBridge();

      sender.sendMessage("Reloaded McServerOtel.");

      return true;
    }

    sender.sendMessage("Usage: /otel reload");

    return true;
  }

  private void startOtelBridge() {
    String serviceName = getConfig().getString("service-name", "minecraft-server");
    String logsEndpoint = getConfig().getString("logs.otlp-endpoint", "http://localhost:4318/v1/logs");
    String metricsEndpoint = getConfig().getString("metrics.otlp-endpoint", "http://localhost:4318/v1/metrics");
    int metricsInterval = getConfig().getInt("metrics.export-interval-seconds", 60);
    boolean playerCountEnabled = getConfig().getBoolean("metrics.types.player-count", true);
    boolean tpsEnabled = getConfig().getBoolean("metrics.types.tps", true);

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
                .setInterval(metricsInterval, java.util.concurrent.TimeUnit.SECONDS)
                .build())
            .build())
        .build();

    Logger otelLogger = otelSdk.getLogsBridge().get("console-otel-bridge");

    appender = new ConsoleLogOtelBridge(otelLogger);
    appender.start();

    getServerConsoleLogger().addAppender(appender);

    if (playerCountEnabled) {
      playerCountGauge = otelSdk.getMeter("mc-server-otel")
          .gaugeBuilder("minecraft.players.online")
          .setDescription("Number of players currently online")
          .setUnit("{players}")
          .buildWithCallback(measurement ->
              measurement.record(getServer().getOnlinePlayers().size()));
    }

    if (tpsEnabled) {
      AttributeKey<String> windowKey = AttributeKey.stringKey("window");
      tpsGauge = otelSdk.getMeter("mc-server-otel")
          .gaugeBuilder("minecraft.server.tps")
          .setDescription("Server ticks per second")
          .setUnit("{tps}")
          .buildWithCallback(measurement -> {
            double[] tps = getServer().getTPS();
            measurement.record(tps[0], Attributes.of(windowKey, "1m"));
            measurement.record(tps[1], Attributes.of(windowKey, "5m"));
            measurement.record(tps[2], Attributes.of(windowKey, "15m"));
          });
    }
  }

  private static org.apache.logging.log4j.core.Logger getServerConsoleLogger() {
    return (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
  }
}
