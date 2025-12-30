package io.github.albertus82.git.config;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.logging.Level;

import org.eclipse.jface.util.Util;

import io.github.albertus82.git.resources.Messages;
import io.github.albertus82.git.util.BuildInfo;
import io.github.albertus82.jface.preference.IPreferencesConfiguration;
import io.github.albertus82.jface.preference.PreferencesConfiguration;
import io.github.albertus82.util.InitializationException;
import io.github.albertus82.util.SystemUtils;
import io.github.albertus82.util.config.Configuration;
import io.github.albertus82.util.config.PropertiesConfiguration;

public class ApplicationConfig extends Configuration {

	private static final String DIRECTORY_NAME;
	private static final String CFG_FILE_NAME = BuildInfo.getProperty("project.groupId") + '.' + BuildInfo.getProperty("project.artifactId") + ".cfg";

	public static class Defaults {
		public static final boolean LOGGING_FILES_ENABLED = true;
		public static final Level LOGGING_LEVEL = Level.INFO;
		public static final int LOGGING_FILES_LIMIT = 0;
		public static final int LOGGING_FILES_COUNT = 1;
		public static final boolean LOGGING_FILES_AUTOCLEAN_ENABLED = true;
		public static final short LOGGING_FILES_AUTOCLEAN_KEEP = 30;

		public static final boolean THRESHOLDS_SPLIT = false;
		public static final String GUI_IMPORTANT_KEYS_SEPARATOR = ",";
		public static final String CONSOLE_SHOW_KEYS_SEPARATOR = ",";
		public static final String THRESHOLDS_EXCLUDED_SEPARATOR = ",";
		public static final String LOGGING_FILES_PATH = SystemUtils.getOsSpecificLocalAppDataDir() + File.separator + DIRECTORY_NAME;

		private Defaults() {
			throw new IllegalAccessError("Constants class");
		}
	}

	static {
		if (Util.isLinux()) {
			DIRECTORY_NAME = '.' + BuildInfo.getProperty("project.artifactId");
		}
		else if (Util.isMac()) {
			DIRECTORY_NAME = "";
		}
		else {
			DIRECTORY_NAME = BuildInfo.getProperty("project.name");
		}
	}

	private static volatile ApplicationConfig instance; // NOSONAR Use a thread-safe type; adding "volatile" is not enough to make this field thread-safe. Use a thread-safe type; adding "volatile" is not enough to make this field thread-safe.
	private static volatile IPreferencesConfiguration wrapper; // NOSONAR Use a thread-safe type; adding "volatile" is not enough to make this field thread-safe. Use a thread-safe type; adding "volatile" is not enough to make this field thread-safe.
	private static int instanceCount = 0;

	private final LanguageConfigAccessor languageConfigAccessor;

	private ApplicationConfig() throws IOException {
		super(new PropertiesConfiguration(DIRECTORY_NAME + File.separator + CFG_FILE_NAME, true));
		final IPreferencesConfiguration pc = new PreferencesConfiguration(this);
		languageConfigAccessor = new LanguageConfigAccessor(pc);
	}

	private static ApplicationConfig getInstance() {
		if (instance == null) {
			synchronized (ApplicationConfig.class) {
				if (instance == null) { // The field needs to be volatile to prevent cache incoherence issues
					try {
						instance = new ApplicationConfig();
						if (++instanceCount > 1) {
							throw new IllegalStateException("Detected multiple instances of singleton " + instance.getClass());
						}
					}
					catch (final IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			}
		}
		return instance;
	}

	public static IPreferencesConfiguration getPreferencesConfiguration() {
		if (wrapper == null) {
			synchronized (ApplicationConfig.class) {
				if (wrapper == null) { // The field needs to be volatile to prevent cache incoherence issues
					wrapper = new PreferencesConfiguration(getInstance());
				}
			}
		}
		return wrapper;
	}

	public static void initialize() {
		try {
			final ApplicationConfig config = getInstance();
			Messages.INSTANCE.setLanguage(config.languageConfigAccessor.getLanguage());
		}
		catch (final RuntimeException e) {
			throw new InitializationException(e);
		}
	}

}
