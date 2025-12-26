package io.github.albertus82.git.config;

import java.util.Locale;

import io.github.albertus82.git.gui.preference.Preference;
import io.github.albertus82.jface.preference.IPreferencesConfiguration;

public class LanguageConfigAccessor {

	public static final String DEFAULT_LANGUAGE = Locale.getDefault().getLanguage();

	/* @NonNull */
	private final IPreferencesConfiguration configuration;

	public LanguageConfigAccessor(IPreferencesConfiguration configuration) {
		this.configuration = configuration;
	}

	public String getLanguage() {
		return configuration.getString(Preference.LANGUAGE, DEFAULT_LANGUAGE);
	}

}
