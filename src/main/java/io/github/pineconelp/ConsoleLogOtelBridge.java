package io.github.pineconelp;

import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.util.concurrent.TimeUnit;

class ConsoleLogOtelBridge extends AbstractAppender {
  private final Logger otelLogger;

  public ConsoleLogOtelBridge(Logger otelLogger) {
    super("ConsoleLogOtelBridge", null, null, true, Property.EMPTY_ARRAY);

    this.otelLogger = otelLogger;
  }

  @Override
  public void append(LogEvent event) {
    otelLogger.logRecordBuilder()
        .setBody(event.getMessage().getFormattedMessage())
        .setSeverity(toSeverity(event.getLevel()))
        .setSeverityText(event.getLevel().name())
        .setTimestamp(event.getInstant().getEpochMillisecond(), TimeUnit.MILLISECONDS)
        .emit();
  }

  private static Severity toSeverity(Level level) {
    if (level == Level.TRACE)
      return Severity.TRACE;
    if (level == Level.DEBUG)
      return Severity.DEBUG;
    if (level == Level.INFO)
      return Severity.INFO;
    if (level == Level.WARN)
      return Severity.WARN;
    if (level == Level.ERROR)
      return Severity.ERROR;
    if (level == Level.FATAL)
      return Severity.FATAL;
    return Severity.INFO;
  }
}
