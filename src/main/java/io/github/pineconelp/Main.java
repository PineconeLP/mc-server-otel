package io.github.pineconelp;

import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import org.apache.logging.log4j.LogManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
  private OpenTelemetrySdk otelSdk;
  private ConsoleLogOtelBridge appender;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    startOtelBridge(
        getConfig().getString("logs.otlp-endpoint", "http://localhost:4318/v1/logs"),
        getConfig().getString("service-name", "minecraft-server"));
  }

  @Override
  public void onDisable() {
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
      startOtelBridge(
          getConfig().getString("logs.otlp-endpoint", "http://localhost:4318/v1/logs"),
          getConfig().getString("service-name", "minecraft-server"));

      sender.sendMessage("Reloaded McServerOtel.");

      return true;
    }

    sender.sendMessage("Usage: /otel reload");

    return true;
  }

  private void startOtelBridge(String endpoint, String serviceName) {
    Resource resource = Resource.getDefault().toBuilder()
        .put(AttributeKey.stringKey("service.name"), serviceName)
        .build();

    otelSdk = OpenTelemetrySdk.builder()
        .setLoggerProvider(SdkLoggerProvider.builder()
            .setResource(resource)
            .addLogRecordProcessor(BatchLogRecordProcessor.builder(
                OtlpHttpLogRecordExporter.builder()
                    .setEndpoint(endpoint)
                    .build())
                .build())
            .build())
        .build();

    Logger otelLogger = otelSdk.getLogsBridge().get("console-otel-bridge");

    appender = new ConsoleLogOtelBridge(otelLogger);
    appender.start();

    getServerConsoleLogger().addAppender(appender);
  }

  private static org.apache.logging.log4j.core.Logger getServerConsoleLogger() {
    return (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
  }
}
