package io.github.albertus82.git.config.logging;

import ch.qos.logback.classic.Level;

public enum LoggingLevel {

	OFF(Level.OFF),
	ERROR(Level.ERROR),
	WARN(Level.WARN),
	INFO(Level.INFO),
	DEBUG(Level.DEBUG),
	TRACE(Level.TRACE),
	ALL(Level.ALL);

	/* @NonNull */
	private final Level level;

	private LoggingLevel(Level level) {
		this.level = level;
	}

	/** Returns the string representation of this Level. */
	@Override
	public String toString() {
		return level.toString();
	}

}
