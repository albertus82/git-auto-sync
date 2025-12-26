package io.github.albertus82.git.engine;

enum SyncState {
	IDLE,
	SYNCING,
	WAITING_FOR_USER,
	BACKOFF
}
