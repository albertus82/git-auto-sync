package io.github.albertus82.git.config;

import java.time.DateTimeException;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.albertus82.git.gui.Preference;

public class TimeZoneConfigAccessor {

	private static final Logger log = LoggerFactory.getLogger(TimeZoneConfigAccessor.class);

	public static final String DEFAULT_ZONE_ID = "UTC";

	public static ZoneId getZoneId() {
		try {
			return ZoneId.of(GitAutoSyncConfig.getPreferencesConfiguration().getString(Preference.TIMEZONE, DEFAULT_ZONE_ID));
		}
		catch (final DateTimeException e) {
			final String fallback = DEFAULT_ZONE_ID;
			log.warn("Cannot determine configured time-zone ID, falling back to " + fallback + ':', e);
			return ZoneId.of(fallback);
		}
	}

}
