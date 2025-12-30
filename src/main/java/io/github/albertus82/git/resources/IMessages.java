package io.github.albertus82.git.resources;

import java.util.Collection;

public interface IMessages {

	String get(/* @NonNull */ String key);

	String get(/* @NonNull */ String key, /* @NonNull */ Object... params);

	Collection<String> getKeys();

}
