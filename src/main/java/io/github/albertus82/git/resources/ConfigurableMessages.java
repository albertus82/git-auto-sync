package io.github.albertus82.git.resources;

public interface ConfigurableMessages extends IMessages {

	Language getLanguage();

	void setLanguage(/* @NonNull */ final Language language);

}
