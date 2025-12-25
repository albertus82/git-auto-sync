package io.github.albertus82.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

public final class GitSyncService {

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
	private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

	public GitSyncService(final Path repoPath, final String username, final String password) {
		this.repoPath = repoPath;
		this.credentials = new UsernamePasswordCredentialsProvider(username, password);
	}

	/*
	 * ======================= Lifecycle =======================
	 */

	public void start() {
		scheduler.scheduleWithFixedDelay(this::syncGuarded, 0, 10, TimeUnit.SECONDS);
	}

	public void stop() {
		scheduler.shutdownNow();
	}

	private void syncGuarded() {
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
		try {
			sync();
		}
		catch (final Exception e) {
			System.err.println("[SYNC ERROR] " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}

	/*
	 * ======================= Sync State Machine =======================
	 */

	private void sync() throws Exception {
		try (final var git = openGit()) {
			final var repo = git.getRepository();

			final boolean merged = recoverIfMerging(git, repo);
			final boolean committed = commitLocalChanges(git);
			pullRemoteChanges(git);
			if (merged || committed) {
				pushChanges(git);
			}
		}
	}

	/*
	 * ======================= Repository Access =======================
	 */

	private Git openGit() throws IOException {
		final var repo = new FileRepositoryBuilder().setGitDir(repoPath.resolve(".git").toFile()).readEnvironment().findGitDir().build();
		return new Git(repo);
	}

	/*
	 * ======================= Merge Recovery =======================
	 */

	private boolean recoverIfMerging(final Git git, final Repository repo) throws Exception {
		if (repo.getRepositoryState() != RepositoryState.MERGING) {
			return false;
		}

		System.out.println("[INFO] Interrupted merge detected");

		resolveConflictsIfAny(git);

		git.commit().setMessage(buildMessage()).call();
		System.out.println("Merged");
		return true;
	}

	/*
	 * ======================= Local Changes =======================
	 */

	private boolean commitLocalChanges(final Git git) throws GitAPIException {
		stageAll(git);

		final var status = git.status().call();
		if (status.isClean()) {
			return false;
		}

		git.commit().setMessage(buildMessage()).call();
		System.out.println("Committed");
		return true;
	}

	private void stageAll(final Git git) throws GitAPIException {
		// Equivalent to: git add -A
		git.add().addFilepattern(".").call();
		git.add().setUpdate(true).call();
	}

	/*
	 * ======================= Pull / Merge =======================
	 */

	private void pullRemoteChanges(final Git git) throws Exception {
		final var result = git.pull().setCredentialsProvider(credentials).setStrategy(MergeStrategy.RECURSIVE).call();

//		if (!result.isSuccessful()) {
//			throw new IllegalStateException("Pull failed");
//		}
		System.out.println("Pulled");

		final var merge = result.getMergeResult();
		if (merge != null && merge.getMergeStatus() == MergeStatus.CONFLICTING) {

			resolveConflictsIfAny(git);

			git.commit().setMessage(buildMessage()).call();
			System.out.println("Merged");
		}
	}

	/*
	 * ======================= Conflict Resolution =======================
	 */

	private void resolveConflictsIfAny(final Git git) throws Exception {
		final var conflicts = git.status().call().getConflicting();
		if (conflicts.isEmpty()) {
			return;
		}

		final var scanner = new Scanner(System.in);

		for (final var path : conflicts) {
			System.out.println("\nConflict: " + path);
			System.out.println("1 = OURS");
			System.out.println("2 = THEIRS");
			System.out.println("3 = BOTH (keep both)");

			final int choice = Integer.parseInt(scanner.nextLine());

			switch (choice) {
			case 1 -> checkoutStage(git, path, Stage.OURS);
			case 2 -> checkoutStage(git, path, Stage.THEIRS);
			case 3 -> {
				createConflictingCopy(git, path);
				checkoutStage(git, path, Stage.THEIRS);
			}
			default -> throw new IllegalArgumentException("Invalid choice");
			}
		}

		stageAll(git);
	}

	private void checkoutStage(Git git, String path, Stage stage) throws GitAPIException {
		git.checkout().addPath(path).setStage(stage).call();
	}

	private void createConflictingCopy(final Git git, final String path) throws Exception {
		final var workTree = git.getRepository().getWorkTree().toPath();
		final var original = workTree.resolve(path);

		final var lastModifiedTime = Files.readAttributes(original, BasicFileAttributes.class).lastModifiedTime();
		final var timestamp = (lastModifiedTime == null ? OffsetDateTime.now() : OffsetDateTime.ofInstant(lastModifiedTime.toInstant(), ZoneId.systemDefault())).toString().replace(":", "-").replace("\\", "-").replace("/", "-");

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
		final var copy = original.resolveSibling(fileNameWithoutExtension + " (conflicted copy of " + clientId + " on " + timestamp + ")" + extension);
		Files.copy(original, copy);
	}

	/*
	 * ======================= Push =======================
	 */

	private void pushChanges(final Git git) throws GitAPIException {
		git.push().setCredentialsProvider(credentials).call();
		System.out.println("Pushed");
	}

	private String buildMessage() {
		return clientId + ' ' + OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
	}

}
