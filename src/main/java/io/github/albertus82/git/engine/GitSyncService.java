package io.github.albertus82.git.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public final class GitSyncService {

	// GUI
	// - if dir exists and is not empty, ask for another directory.
	// - if dir not exists, mkdir.
	// - if dir exists and is empty, mkdir and ask for remote URL, username and password, and clone.
	// - check if .gitattributes exists, if not, ask permission to create it as "* binary"

	private static final String gitattributes = "* binary"; // Needed to make conflicted copy work correctly

	private static final DateTimeFormatter logTimestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	public static void main(final String... args) {
		final var repoPath = Path.of(args[0].trim());
		final var username = args[1].trim();
		final var password = args[2].trim();

		final var service = new GitSyncService(repoPath, username, password);
		service.start();

		Runtime.getRuntime().addShutdownHook(new Thread(service::stop));
	}

	private final String clientId = UUID.randomUUID().toString().replace("-", "");
	private final Path repoPath;
	private final CredentialsProvider credentials;
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private final AtomicReference<SyncState> state = new AtomicReference<>(SyncState.IDLE);
	private final AtomicBoolean syncInProgress = new AtomicBoolean(false);
	private final AtomicLong lastPull = new AtomicLong(0);
	private final AtomicBoolean pushRequired = new AtomicBoolean(false);

	public GitSyncService(final Path repoPath, final String username, final String password) {
		this.repoPath = repoPath;
		this.credentials = new UsernamePasswordCredentialsProvider(username, password);
	}

	public void start() {
		scheduler.scheduleWithFixedDelay(this::syncGuarded, 0, 5, TimeUnit.SECONDS);
	}

	public void stop() {
		scheduler.shutdownNow();
	}

	private void syncGuarded() {
		var current = state.get();

		if (current == SyncState.WAITING_FOR_USER) {
			return; // intentional pause
		}

		if (!syncInProgress.compareAndSet(false, true)) {
			return;
		}
		try {
			syncSafely();
		}
		finally {
			syncInProgress.set(false);
		}
	}

	private void syncSafely() {
		state.set(SyncState.SYNCING);
		try {
			sync();
		}
		catch (final UserInteractionRequired e) {
			// ok
		}
		catch (final Exception e) {
			e.printStackTrace();
		}
		finally {
			if (state.get() == SyncState.SYNCING) {
				state.set(SyncState.IDLE);
			}
		}
	}

	private void sync() throws Exception {
		try (final var git = openGit()) {
			final var repo = git.getRepository();

			final boolean merged = recoverIfMerging(git, repo);
			final boolean committed = commitLocalChanges(git);

			if (committed || Instant.ofEpochMilli(lastPull.get()).isBefore(Instant.now().minus(Duration.of(60, ChronoUnit.SECONDS)))) {
				lastPull.set(Instant.now().toEpochMilli());
				pullRemoteChanges(git);
			}
			if (merged || committed || pushRequired.getAndSet(false)) {
				pushChanges(git);
			}
		}
	}

	private Git openGit() throws IOException {
		final var repo = new FileRepositoryBuilder().setGitDir(repoPath.resolve(".git").toFile()).readEnvironment().findGitDir().build();
		return new Git(repo);
	}

	private boolean recoverIfMerging(final Git git, final Repository repo) throws Exception {
		if (repo.getRepositoryState() != RepositoryState.MERGING) {
			return false;
		}

		System.out.println(logTimestampFormat.format(LocalDateTime.now()) + " - Interrupted merge detected");

		resolveConflictsIfAny(git);

		git.commit().setMessage(buildMessage()).call();
		System.out.println(logTimestampFormat.format(LocalDateTime.now()) + " - Merged");
		return true;
	}

	private boolean commitLocalChanges(final Git git) throws GitAPIException {
		stageAll(git);

		final var status = git.status().call();
		System.out.println(logTimestampFormat.format(LocalDateTime.now()) + " - Status");
		if (status.isClean()) {
			return false;
		}

		git.commit().setMessage(buildMessage()).call();
		System.out.println(logTimestampFormat.format(LocalDateTime.now()) + " - Committed");
		return true;
	}

	private void stageAll(final Git git) throws GitAPIException {
		// Equivalent to: git add -A
		git.add().addFilepattern(".").call();
		git.add().setUpdate(true).call();
	}

	private void pullRemoteChanges(final Git git) throws Exception {
		final var result = git.pull().setCredentialsProvider(credentials).setStrategy(MergeStrategy.RECURSIVE).call();
		System.out.println(logTimestampFormat.format(LocalDateTime.now()) + " - Pulled");

		final var merge = result.getMergeResult();
		if (merge != null && merge.getMergeStatus() == MergeStatus.CONFLICTING) {

			resolveConflictsIfAny(git);

			git.commit().setMessage(buildMessage()).call();
			System.out.println(logTimestampFormat.format(LocalDateTime.now()) + " - Merged");
		}
	}

	public static int openConflictDialog(final Shell parentShell) {
		final var buttons = new TreeMap<Integer, String>(Map.of(0, "Keep ours", 1, "Keep theirs", 2, "Keep both"));

		final var dialog = new MessageDialog(parentShell, "Conflict", // dialog title
				null, // no custom image
				"How to resolve?", // message
				MessageDialog.QUESTION, buttons.values().toArray(new String[0]), 2 // default button index
		);

		return dialog.open();

	}

	private void resolveConflictsIfAny(final Git git) throws Exception {
		final var conflicts = git.status().call().getConflicting();
		if (conflicts.isEmpty()) {
			return;
		}

		state.set(SyncState.WAITING_FOR_USER);

		final var iterator = conflicts.iterator();
		startConflictResolutionSession(git, iterator);

		throw new UserInteractionRequired();
	}

	private void startConflictResolutionSession(final Git git, final Iterator<String> conflicts) {
		Display.getDefault().asyncExec(() -> runConflictStep(git, conflicts));
	}

	private void runConflictStep(final Git git, final Iterator<String> conflicts) {
		if (!conflicts.hasNext()) {
			completeConflictResolution(git);
			return;
		}

		final var path = conflicts.next();
		// Shell shell = getActiveShell();
		final var display = Display.getCurrent();
		if (display == null) {
			return;
		}
		final var shell = display.getShells().length > 0 ? display.getShells()[0] : null;
		if (shell == null) {
			return;
		}

		final var choice = ConflictResolutionDialog.ask(shell, path);

		try {
			applyConflictChoice(git, path, choice);
		}
		catch (final Exception e) {
			onSyncFailure(e);
			return;
		}

		// Schedule next step explicitly
		Display.getDefault().asyncExec(() -> runConflictStep(git, conflicts));
	}

	private void completeConflictResolution(final Git git) {
		try {
			stageAll(git);
			git.commit().setMessage(buildMessage()).call();
			System.out.println(logTimestampFormat.format(LocalDateTime.now()) + " - Merged");
			pushRequired.set(true);
		}
		catch (final Exception e) {
			onSyncFailure(e);
			return;
		}

		state.set(SyncState.IDLE);
		scheduler.execute(this::syncGuarded);
	}

	private void applyConflictChoice(final Git git, final String path, final ConflictChoice choice) throws Exception {
		switch (choice) {
		case OURS -> checkoutStage(git, path, Stage.OURS);
		case THEIRS -> checkoutStage(git, path, Stage.THEIRS);
		case BOTH -> {
			createConflictingCopy(git, path);
			checkoutStage(git, path, Stage.THEIRS);
		}
		}
	}

	private void checkoutStage(final Git git, final String path, final Stage stage) throws GitAPIException {
		git.checkout().addPath(path).setStage(stage).call();
	}

	private void createConflictingCopy(final Git git, final String path) throws Exception {
		final var workTree = git.getRepository().getWorkTree().toPath();
		final var original = workTree.resolve(path);

		final var lastModifiedTime = Files.readAttributes(original, BasicFileAttributes.class).lastModifiedTime();
		final var timestamp = (lastModifiedTime == null ? OffsetDateTime.now() : OffsetDateTime.ofInstant(lastModifiedTime.toInstant(), ZoneId.systemDefault())).toString().replace('.', '-').replace(':', '-').replace('\\', '-').replace('/', '-');

		String fileNameWithoutExtension;
		String extension;
		final var originalFileName = original.getFileName().toString();
		final var lastDotIndex = originalFileName.lastIndexOf('.');
		if (lastDotIndex != -1 && lastDotIndex != originalFileName.length() - 1) {
			fileNameWithoutExtension = originalFileName.substring(0, lastDotIndex);
			extension = originalFileName.substring(lastDotIndex);
		}
		else {
			fileNameWithoutExtension = originalFileName;
			extension = "";
		}
		final var copy = original.resolveSibling(fileNameWithoutExtension + " (conflicted copy of " + clientId + " on " + timestamp + ')' + extension);
		Files.copy(original, copy);
	}

	private void onSyncFailure(final Exception e) {
		e.printStackTrace();
	}

	private void pushChanges(final Git git) throws GitAPIException {
		git.push().setCredentialsProvider(credentials).call();
		System.out.println(logTimestampFormat.format(LocalDateTime.now()) + " - Pushed");
	}

	private String buildMessage() {
		return clientId + ' ' + OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
	}

}
