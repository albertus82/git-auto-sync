package io.github.albertus82.git.gui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.albertus82.git.config.GitAutoSyncConfig;
import io.github.albertus82.git.resources.Messages;
import io.github.albertus82.git.util.BuildInfo;
import io.github.albertus82.jface.EnhancedErrorDialog;
import io.github.albertus82.jface.Multilanguage;
import io.github.albertus82.util.InitializationException;

public class GitAutoSyncGui extends ApplicationWindow implements Multilanguage {

	private static final Logger log = LoggerFactory.getLogger(GitAutoSyncGui.class);

	private GitAutoSyncGui() {
		super(null);
		//addStatusLine();
	}

	@Override
	public void updateLanguage() {
		// TODO Auto-generated method stub

	}

	public static void main(final String... args) {
		try {
			Display.setAppName(getApplicationName());
			Display.setAppVersion(BuildInfo.getProperty("project.version"));
			Window.setDefaultImages(Images.getAppIconArray());
			start();
		}
		catch (final RuntimeException | Error e) { // NOSONAR Catch Exception instead of Error. Throwable and Error should not be caught (java:S1181)
			log.error("An unrecoverable error has occurred:", e);
			throw e;
		}
	}

	private static void start() {
		Shell shell = null;
		try {
			GitAutoSyncConfig.initialize(); // Load configuration and initialize the application
			final var gui = new GitAutoSyncGui();
			gui.open(); // Open main window
			shell = gui.getShell();
			loop(shell);
		}
		catch (final InitializationException e) {
			EnhancedErrorDialog.openError(shell, getApplicationName(), Messages.get("error.fatal.init"), IStatus.ERROR, e, Images.getAppIconArray());
			throw e;
		}
		catch (final RuntimeException e) {
			if (shell != null && shell.isDisposed()) {
				log.debug("An unrecoverable error has occurred:", e);
				// Do not rethrow, exiting with status OK.
			}
			else {
				EnhancedErrorDialog.openError(shell, getApplicationName(), Messages.get("error.fatal"), IStatus.ERROR, e, Images.getAppIconArray());
				throw e;
			}
		}
	}

	private static void loop(/* @NonNull */ final Shell shell) {
		final var display = shell.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.isDisposed() && !display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	public static String getApplicationName() {
		return "git auto sync";//  Messages.get("message.application.name");
	}
}
