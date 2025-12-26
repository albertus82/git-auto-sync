package io.github.albertus82.git.resources;

import io.github.albertus82.git.config.LanguageConfigAccessor;
import io.github.albertus82.util.ILanguageManager;

public class LanguageManager implements ILanguageManager {

	public LanguageManager(LanguageConfigAccessor languageConfig) {
		this.languageConfig = languageConfig;
	}

	/* @NonNull */
	private final LanguageConfigAccessor languageConfig;

	@Override
	public void resetLanguage() {
		Messages.setLanguage(languageConfig.getLanguage());
	}

}
