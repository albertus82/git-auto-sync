package io.github.albertus82.git.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.UUID;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.albertus82.git.config.ApplicationConfig;
import io.github.albertus82.git.engine.GitSyncService;
import io.github.albertus82.git.gui.listener.ExitListener;
import io.github.albertus82.git.gui.preference.Preference;
import io.github.albertus82.git.resources.ConfigurableMessages;
import io.github.albertus82.git.resources.Messages;
import io.github.albertus82.git.util.BuildInfo;
import io.github.albertus82.jface.EnhancedErrorDialog;
import io.github.albertus82.jface.Events;
import io.github.albertus82.jface.Multilanguage;
import io.github.albertus82.jface.SwtUtils;
import io.github.albertus82.jface.console.StyledTextConsole;
import io.github.albertus82.jface.preference.IPreferencesConfiguration;
import io.github.albertus82.util.InitializationException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class GitAutoSyncGui extends ApplicationWindow implements Multilanguage {

	public static final String SHELL_MAXIMIZED = "shell.maximized";
	private static final String SHELL_SIZE_X = "shell.size.x";
	private static final String SHELL_SIZE_Y = "shell.size.y";
	private static final String SHELL_LOCATION_X = "shell.location.x";
	private static final String SHELL_LOCATION_Y = "shell.location.y";
	private static final Point POINT_ZERO = new Point(0, 0);

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class Defaults {
		public static final boolean START_MINIMIZED = false;
		public static final boolean SYNC_ON_START = false;
		public static final boolean SHELL_MAXIMIZED = false;
	}

	private static final Logger log = LoggerFactory.getLogger(GitAutoSyncGui.class);

	private static final ConfigurableMessages messages = Messages.INSTANCE;

	private final IPreferencesConfiguration configuration = ApplicationConfig.getPreferencesConfiguration();

	private final Collection<Multilanguage> multilanguages = new ArrayList<>();

	@Getter
	private TrayIcon trayIcon;

	@Getter
	private MenuBar menuBar;

	@Getter
	private StyledTextConsole console;

	/** Shell maximized status. May be null in some circumstances. */
	private Boolean shellMaximized;

	/** Shell size. May be null in some circumstances. */
	private Point shellSize;

	/** Shell location. May be null in some circumstances. */
	private Point shellLocation;

	private GitAutoSyncGui() {
		super(null);
		addStatusLine();
	}

	@Override
	public void updateLanguage() {
		final Shell shell = getShell();
		shell.setRedraw(false);

		for (final Multilanguage element : multilanguages) {
			element.updateLanguage();
		}

		//		final TableColumn[] columns = resultsTable.getTableViewer().getTable().getColumns();
		//		final int[] widths = new int[columns.length];
		//		for (int i = 0; i < columns.length; i++) {
		//			widths[i] = columns[i].getWidth();
		//			columns[i].setWidth(1);
		//		}

		shell.layout(true, true);
		shell.setMinimumSize(shell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x, shell.getMinimumSize().y);

		//		for (int i = 0; i < columns.length; i++) {
		//			columns[i].setWidth(widths[i]);
		//		}

		shell.setRedraw(true);
	}

	public static void main(final String... args) throws IOException {
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

	private static void start(final String... args) throws IOException {
		Shell shell = null;
		GitSyncService service = null;
		try {
			ApplicationConfig.initialize(); // Load configuration and initialize the application
			final var gui = new GitAutoSyncGui();
			gui.open(); // Open main window
			shell = gui.getShell();

			final var configuration = ApplicationConfig.getPreferencesConfiguration();
			var properties = configuration.getProperties();

			if (properties.getProperty("repo.path", "").isBlank() || properties.getProperty("repo.username", "").isBlank() || properties.getProperty("repo.password", "").isBlank()) {
				gui.getMenuBar().getPreferencesListener().handleEvent(null);
			}
			properties = configuration.getProperties();
			if (properties.getProperty("client.id", "").isBlank()) {
				properties.setProperty("client.id", UUID.randomUUID().toString().replace("-", ""));
			}
			new Thread(() -> { // don't perform I/O in UI thread
				try {
					configuration.save();
				}
				catch (final IOException e) {
					log.warn("Cannot save configuration:", e);
				}
			});

			service = new GitSyncService();
			service.start();

			loop(shell);
		}
		catch (final InitializationException | IOException e) {
			EnhancedErrorDialog.openError(shell, getApplicationName(), messages.get("error.fatal.init"), IStatus.ERROR, e, Images.getAppIconArray());
			throw e;
		}
		catch (final RuntimeException e) {
			if (shell != null && shell.isDisposed()) {
				log.debug("An unrecoverable error has occurred:", e);
				// Do not rethrow, exiting with status OK.
			}
			else {
				EnhancedErrorDialog.openError(shell, getApplicationName(), messages.get("error.fatal"), IStatus.ERROR, e, Images.getAppIconArray());
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
		trayIcon = new TrayIcon(this);
		multilanguages.add(trayIcon);

		menuBar = new MenuBar(this);
		multilanguages.add(menuBar);

		console = new StyledTextConsole(parent, GridDataFactory.fillDefaults().grab(true, true).create(), true);
		final String fontDataString = configuration.getString("gui.console.font", true);
		if (!fontDataString.isEmpty()) {
			console.setFont(PreferenceConverter.readFontData(fontDataString));
		}
		console.setLimit(() -> configuration.getInt("gui.console.max.chars"));

		return parent;
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

	@Override
	public int open() {
		final int code = super.open();

		final UpdateShellStatusListener listener = new UpdateShellStatusListener();
		getShell().addListener(SWT.Resize, listener);
		getShell().addListener(SWT.Move, listener);
		getShell().addListener(SWT.Activate, new MaximizeShellListener());
		getShell().addListener(SWT.Deactivate, new DeactivateShellListener());

		if (SwtUtils.isGtk3() == null || SwtUtils.isGtk3()) { // fixes invisible (transparent) shell bug with some Linux distibutions
			setMinimizedMaximizedShellStatus();
		}

		//		for (final Button radio : searchForm.getFormatRadios().values()) {
		//			if (radio.getSelection()) {
		//				radio.notifyListeners(SWT.Selection, null);
		//				break;
		//			}
		//		}
		if (configuration.getBoolean(Preference.SYNC_ON_START, Defaults.SYNC_ON_START)) {
			//searchForm.getSearchButton().notifyListeners(SWT.Selection, null);
		}
		return code;
	}

	@Override
	protected void constrainShellSize() {
		super.constrainShellSize();
		final Shell shell = getShell();
		final Point preferredSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		shell.setMinimumSize(preferredSize);

		final Integer sizeX = configuration.getInt(SHELL_SIZE_X);
		final Integer sizeY = configuration.getInt(SHELL_SIZE_Y);
		if (sizeX != null && sizeY != null) {
			shell.setSize(Math.max(sizeX, preferredSize.x), Math.max(sizeY, preferredSize.y));
		}

		final Integer locationX = configuration.getInt(SHELL_LOCATION_X);
		final Integer locationY = configuration.getInt(SHELL_LOCATION_Y);
		if (locationX != null && locationY != null) {
			if (new Rectangle(locationX, locationY, shell.getSize().x, shell.getSize().y).intersects(shell.getDisplay().getBounds())) {
				shell.setLocation(locationX, locationY);
			}
			else {
				log.warn("Illegal shell location ({}, {}) for size ({}).", locationX, locationY, shell.getSize());
			}
		}

		if (SwtUtils.isGtk3() != null && !SwtUtils.isGtk3()) { // fixes invisible (transparent) shell bug with some Linux distibutions
			setMinimizedMaximizedShellStatus();
		}
	}

	@Override
	protected void handleShellCloseEvent() {
		final Event event = new Event();
		new ExitListener(this).handleEvent(event);
		if (event.doit) {
			super.handleShellCloseEvent();
		}
	}

	private void setMinimizedMaximizedShellStatus() {
		if (configuration.getBoolean(Preference.START_MINIMIZED, Defaults.START_MINIMIZED)) {
			getShell().setMinimized(true);
		}
		else if (configuration.getBoolean(SHELL_MAXIMIZED, Defaults.SHELL_MAXIMIZED)) {
			getShell().setMaximized(true);
		}
	}

	private class UpdateShellStatusListener implements Listener {
		@Override
		public void handleEvent(final Event event) {
			logEvent(event);
			final Shell shell = getShell();
			if (shell != null && !shell.isDisposed()) {
				shellMaximized = shell.getMaximized();
				if (Boolean.FALSE.equals(shellMaximized) && !POINT_ZERO.equals(shell.getSize())) {
					shellSize = shell.getSize();
					shellLocation = shell.getLocation();
				}
			}
			log.debug("shellMaximized: {} - shellSize: {} - shellLocation: {}", shellMaximized, shellSize, shellLocation);
		}
	}

	private class MaximizeShellListener implements Listener {
		private boolean firstTime = true;

		@Override
		public void handleEvent(final Event event) {
			logEvent(event);
			if (firstTime && !getShell().isDisposed() && !configuration.getBoolean(Preference.MINIMIZE_TRAY, TrayIcon.Defaults.MINIMIZE_TRAY) && configuration.getBoolean(Preference.START_MINIMIZED, Defaults.START_MINIMIZED) && configuration.getBoolean(SHELL_MAXIMIZED, Defaults.SHELL_MAXIMIZED)) {
				firstTime = false;
				getShell().setMaximized(true);
			}
		}
	}

	private class DeactivateShellListener implements Listener {
		private boolean firstTime = true;

		@Override
		public void handleEvent(final Event event) {
			logEvent(event);
			if (firstTime && configuration.getBoolean(Preference.START_MINIMIZED, Defaults.START_MINIMIZED)) {
				firstTime = false;
			}
			else {
				saveShellStatus();
			}
		}
	}

	private static void logEvent(final Event event) {
		log.debug("{} {}", Events.getName(event), event);
	}

	public void saveShellStatus() {
		new Thread(() -> { // don't perform I/O in UI thread
			try {
				configuration.reload(); // make sure the properties are up-to-date
			}
			catch (final IOException e) {
				log.warn("Cannot reload configuration:", e);
				return; // abort
			}
			final Properties properties = configuration.getProperties();

			if (shellMaximized != null) {
				properties.setProperty(SHELL_MAXIMIZED, Boolean.toString(shellMaximized));
			}
			if (shellSize != null) {
				properties.setProperty(SHELL_SIZE_X, Integer.toString(shellSize.x));
				properties.setProperty(SHELL_SIZE_Y, Integer.toString(shellSize.y));
			}
			if (shellLocation != null) {
				properties.setProperty(SHELL_LOCATION_X, Integer.toString(shellLocation.x));
				properties.setProperty(SHELL_LOCATION_Y, Integer.toString(shellLocation.y));
			}

			log.debug("{}", configuration);

			try {
				configuration.save(); // save configuration
			}
			catch (final IOException e) {
				log.warn("Cannot save configuration:", e);
			}
		}, "Save shell status").start();
	}

	public static String getApplicationName() {
		return messages.get("message.application.name");
	}

}
