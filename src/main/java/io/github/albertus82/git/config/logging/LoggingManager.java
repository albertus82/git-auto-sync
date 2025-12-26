package io.github.albertus82.git.config.logging;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import io.github.albertus82.util.logging.ILoggingManager;

public class LoggingManager implements ILoggingManager {

	/* @NonNull */
	private final LoggingConfigAccessor currentConfig;

	private LoggingConfigValue previousConfig;

	public LoggingManager(LoggingConfigAccessor currentConfig) {
		this.currentConfig = currentConfig;
	}

	@Override
	public void initializeLogging() {
		if (previousConfig == null) {
			previousConfig = new LoggingConfigValue(currentConfig);
		}
		else if (!new LoggingConfigValue(currentConfig).equals(previousConfig)) {
			previousConfig = new LoggingConfigValue(currentConfig);
			final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
			context.reset();
			try {
				new ContextInitializer(context).autoConfig();
			}
			catch (final JoranException e) {
				// StatusPrinter will handle this
			}
			StatusPrinter.printInCaseOfErrorsOrWarnings(context);
		}
	}

	private class LoggingConfigValue implements LoggingConfig {

		private final Level consoleLevel;
		private final boolean fileAppenderEnabled;
		private final boolean fileCompressionEnabled;
		private final Level fileLevel;
		private final byte fileMaxIndex;
		private final int fileMaxSize;
		private final String fileNamePattern;
		private final String layoutPattern;
		private final Level rootLevel;

		private LoggingConfigValue(/* @NonNull */ final LoggingConfigAccessor config) {
			this.consoleLevel = config.getConsoleLevel();
			this.fileLevel = config.getFileLevel();
			this.fileMaxIndex = config.getFileMaxIndex();
			this.fileMaxSize = config.getFileMaxSize();
			this.fileNamePattern = config.getFileNamePattern();
			this.layoutPattern = config.getLayoutPattern();
			this.rootLevel = config.getRootLevel();
			this.fileAppenderEnabled = config.isFileAppenderEnabled();
			this.fileCompressionEnabled = config.isFileCompressionEnabled();
		}

		public Level getConsoleLevel() {
			return consoleLevel;
		}

		public boolean isFileAppenderEnabled() {
			return fileAppenderEnabled;
		}

		public boolean isFileCompressionEnabled() {
			return fileCompressionEnabled;
		}

		public Level getFileLevel() {
			return fileLevel;
		}

		public byte getFileMaxIndex() {
			return fileMaxIndex;
		}

		public int getFileMaxSize() {
			return fileMaxSize;
		}

		public String getFileNamePattern() {
			return fileNamePattern;
		}

		public String getLayoutPattern() {
			return layoutPattern;
		}

		public Level getRootLevel() {
			return rootLevel;
		}
	}

}
