package io.github.pineconelp.metrics;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

public class LogCountMetric extends AbstractAppender implements MinecraftMetric {
  private static final AttributeKey<String> SEVERITY_KEY = AttributeKey.stringKey("severity");

  private final Logger rootLogger;
  private LongCounter counter;

  public LogCountMetric() {
    super("LogCountMetric", null, null, true, Property.EMPTY_ARRAY);
    this.rootLogger = (Logger) LogManager.getRootLogger();
  }

  @Override
  public void register(Meter meter) {
    counter = meter.counterBuilder("minecraft.logs.count")
        .setDescription("Number of log events by severity")
        .setUnit("{logs}")
        .build();

    start();
    rootLogger.addAppender(this);
  }

  @Override
  public void append(LogEvent event) {
    counter.add(1, Attributes.of(SEVERITY_KEY, event.getLevel().name()));
  }

  @Override
  public void close() {
    rootLogger.removeAppender(this);
    stop();
  }
}
