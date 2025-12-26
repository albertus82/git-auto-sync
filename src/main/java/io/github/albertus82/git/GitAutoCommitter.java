//package io.github.albertus82.git;
//
//import java.io.File;
//import java.time.ZonedDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.Locale;
//import java.util.concurrent.TimeUnit;
//
//import org.eclipse.jgit.api.CheckoutCommand.Stage;
//import org.eclipse.jgit.api.Git;
//import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
//import org.eclipse.jgit.merge.ContentMergeStrategy;
//import org.eclipse.jgit.merge.MergeStrategy;
//import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
//
//public class GitAutoCommitter {
//
//
//	public static void main(final String... args) throws Exception {
//		final var credentialsProvider = new UsernamePasswordCredentialsProvider(GIT_USERNAME, GIT_PASSWORD);
//
//		// Clone the repository if it doesn't exist
//		final var repoDir = new File(GIT_LOCAL_PATH);
//		if (!repoDir.exists()) {
//			Git.cloneRepository().setURI(GIT_REPO_URL).setDirectory(repoDir).setCredentialsProvider(credentialsProvider).call();
//		}
//
//		// Git repository instance
//		final var git = Git.open(repoDir);
//
//		// Per prima cosa fa pull
//		try {
//			final var pull1 = git.pull().setCredentialsProvider(credentialsProvider)     .setContentMergeStrategy(ContentMergeStrategy.CONFLICT).call();
//			System.out.println(pull1);
//			if (pull1.getMergeResult() != null) {
//				final var conflicts = pull1.getMergeResult().getConflicts();
//				System.out.println(conflicts);
//				if (conflicts != null && !conflicts.isEmpty()) {
//					for (final var path : conflicts.keySet()) {
//						git.checkout().setStage(Stage.valueOf(System.console().readLine().toUpperCase(Locale.ROOT))).addPath(path);
//					}
//					final var merge = git.merge().setCommit(true);
//					System.out.println(merge);
//				}
//				final var push = git.push().setCredentialsProvider(credentialsProvider).setForce(true).call();
//				System.out.println(push);
//			}
//
//		}
//		catch (WrongRepositoryStateException e) {
//			System.err.println(e);
//
//			final var pull1 = git.merge(). setContentMergeStrategy(ContentMergeStrategy.CONFLICT).call();
//			System.out.println(pull1);
//			final var conflicts = pull1.getConflicts();
//			System.out.println(conflicts);
//			if (conflicts != null && !conflicts.isEmpty()) {
//				for (final var path : conflicts.keySet()) {
//					git.checkout().setStage(Stage.valueOf(System.console().readLine().toUpperCase(Locale.ROOT))).addPath(path);
//				}
//				final var merge = git.merge().setCommit(true);
//				System.out.println(merge);
//			}
//			final var push = git.push().setCredentialsProvider(credentialsProvider).setForce(true).call();
//			System.out.println(push);
//
//		}
//
//		// Infinite loop to keep the watcher alive
//		while (true) {
//			TimeUnit.SECONDS.sleep(30); // You can adjust this to make it more efficient
//			final var status = git.status().call();
//			if (status.hasUncommittedChanges() || !status.getUntracked().isEmpty() || !status.getUntrackedFolders().isEmpty()) { // Se e' cambiato qualcosa in locale
//				final var add = git.add().setAll(true).call();
//				System.out.println(add);
//				final var commit1 = git.commit().setMessage(DateTimeFormatter.ISO_ZONED_DATE_TIME.format(ZonedDateTime.now())).call();
//				System.out.println(commit1);
//				final var pull2 = git.pull().setCredentialsProvider(credentialsProvider).setContentMergeStrategy(ContentMergeStrategy.CONFLICT).call();
//				System.out.println(pull2);
//				if (pull2.getMergeResult() != null) {
//					final var conflicts2 = pull2.getMergeResult().getConflicts();
//					System.out.println(conflicts2);
//					if (conflicts2 != null && !conflicts2.isEmpty()) {
//						for (final var path : conflicts2.keySet()) {
//							git.checkout().setStage(Stage.valueOf(System.console().readLine().toUpperCase(Locale.ROOT))).addPath(path);
//						}
//						final var merge = git.merge().setCommit(true);
//						System.out.println(merge);
//						//final var push = git.push().setCredentialsProvider(credentialsProvider).setForce(true).call();
//						//System.out.println(push);
//					}
//				}
//
//				//				if (pull.getMergeResult() != null && pull.getMergeResult().getMergeStatus() != MergeStatus.ALREADY_UP_TO_DATE) {
//				//					final var commit2 = git.commit().setMessage("Merge " + DateTimeFormatter.ISO_ZONED_DATE_TIME.format(ZonedDateTime.now())).call();
//				//					System.out.println(commit2);
//				//				}
//				final var push = git.push().setCredentialsProvider(credentialsProvider).setForce(true).call();
//				System.out.println(push);
//			}
//			else { // Se non ci sono variazioni in locale
//				final var pull = git.pull().setCredentialsProvider(credentialsProvider).setContentMergeStrategy(ContentMergeStrategy.CONFLICT).call();
//				System.out.println(pull);
//			}
//		}
//	}
//
//}
