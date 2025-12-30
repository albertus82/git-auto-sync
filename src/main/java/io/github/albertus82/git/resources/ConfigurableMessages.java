package io.github.albertus82.git.resources;

import lombok.NonNull;

public interface ConfigurableMessages extends IMessages {

	Language getLanguage();

	void setLanguage(/* @NonNull */ final Language language);

}
