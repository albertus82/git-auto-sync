package io.github.albertus82.git.gui;

import java.nio.file.Path;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.albertus82.git.config.GitAutoSyncConfig;
import io.github.albertus82.git.engine.GitSyncService;
import io.github.albertus82.git.resources.Messages;
import io.github.albertus82.git.util.BuildInfo;
import io.github.albertus82.jface.EnhancedErrorDialog;
import io.github.albertus82.jface.Multilanguage;
import io.github.albertus82.jface.console.StyledTextConsole;
import io.github.albertus82.jface.preference.IPreferencesConfiguration;
import io.github.albertus82.util.InitializationException;

public class GitAutoSyncGui extends ApplicationWindow implements Multilanguage {

	private static final Logger log = LoggerFactory.getLogger(GitAutoSyncGui.class);

	private final IPreferencesConfiguration configuration = GitAutoSyncConfig.getPreferencesConfiguration();

	private StyledTextConsole console;

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
			start(args);
		}
		catch (final RuntimeException | Error e) { // NOSONAR Catch Exception instead of Error. Throwable and Error should not be caught (java:S1181)
			log.error("An unrecoverable error has occurred:", e);
			throw e;
		}
	}

	private static void start(final String... args) {
		Shell shell = null;
		GitSyncService service = null;
		try {
			GitAutoSyncConfig.initialize(); // Load configuration and initialize the application
			final var gui = new GitAutoSyncGui();
			gui.open(); // Open main window
			shell = gui.getShell();

			final var repoPath = Path.of(args[0].trim());
			final var username = args[1].trim();
			final var password = args[2].trim();

			service = new GitSyncService(repoPath, username, password);
			service.start();

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
		finally {
			if (service != null) {
				service.stop();
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

	@Override
	protected Control createContents(final Composite parent) {
		console = new StyledTextConsole(parent, new GridData(SWT.FILL, SWT.FILL, true, true), true);
		final String fontDataString = configuration.getString("gui.console.font", true);
		if (!fontDataString.isEmpty()) {
			console.setFont(PreferenceConverter.readFontData(fontDataString));
		}
		console.setLimit(() -> configuration.getInt("gui.console.max.chars"));

		return super.createContents(parent);
	}

	@Override
	protected void configureShell(final Shell shell) {
		super.configureShell(shell);
		shell.setText(getApplicationName());
	}

	@Override
	protected void initializeBounds() {/* Do not pack the shell */}

	@Override
	protected void createTrimWidgets(final Shell shell) {/* Not needed */}

	@Override
	protected Layout getLayout() {
		return GridLayoutFactory.swtDefaults().create();
	}

	public static String getApplicationName() {
		return "git auto sync";//  Messages.get("message.application.name");
	}

}
