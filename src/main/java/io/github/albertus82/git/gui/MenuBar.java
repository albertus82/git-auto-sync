package io.github.albertus82.git.gui;

import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import io.github.albertus82.git.gui.listener.AboutListener;
import io.github.albertus82.git.gui.listener.ArmMenuListener;
import io.github.albertus82.git.gui.listener.ExitListener;
import io.github.albertus82.git.gui.listener.PreferencesListener;
import io.github.albertus82.git.resources.Messages;
import io.github.albertus82.jface.Multilanguage;
import io.github.albertus82.jface.cocoa.CocoaEnhancerException;
import io.github.albertus82.jface.cocoa.CocoaUIEnhancer;
import io.github.albertus82.jface.i18n.LocalizedWidgets;
import io.github.albertus82.jface.sysinfo.SystemInformationDialog;
import io.github.albertus82.util.ISupplier;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Solo i <tt>MenuItem</tt> che fanno parte di una barra dei men&ugrave; con
 * stile <tt>SWT.BAR</tt> hanno gli acceleratori funzionanti; negli altri casi
 * (ad es. <tt>SWT.POP_UP</tt>), bench&eacute; vengano visualizzate le
 * combinazioni di tasti, gli acceleratori non funzioneranno e le relative
 * combinazioni di tasti saranno ignorate.
 */
@Slf4j
public class MenuBar implements Multilanguage {

	@Getter
	private final PreferencesListener preferencesListener;

	MenuBar(@NonNull final GitAutoSyncGui gui) {
		final Shell shell = gui.getShell();

		final ExitListener exitListener = new ExitListener(gui);
		final AboutListener aboutListener = new AboutListener(gui);
		preferencesListener = new PreferencesListener(gui);

		boolean cocoaMenuCreated = false;

		if (Util.isCocoa()) {
			try {
				new CocoaUIEnhancer(shell.getDisplay()).hookApplicationMenu(exitListener, aboutListener, preferencesListener);
				cocoaMenuCreated = true;
			}
			catch (final CocoaEnhancerException e) {
				log.warn("Unable to enhance Cocoa UI:", e);
			}
		}

		final Menu bar = new Menu(shell, SWT.BAR); // Bar

		if (!cocoaMenuCreated) {
			// File
			final Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
			final MenuItem fileMenuHeader = newLocalizedMenuItem(bar, SWT.CASCADE, "label.menu.header.file");
			fileMenuHeader.setMenu(fileMenu);

			final MenuItem toolsPreferencesMenuItem = newLocalizedMenuItem(fileMenu, SWT.PUSH, "label.menu.item.preferences");
			toolsPreferencesMenuItem.addSelectionListener(new PreferencesListener(gui));

			new MenuItem(fileMenu, SWT.SEPARATOR);

			final MenuItem fileExitItem = newLocalizedMenuItem(fileMenu, SWT.PUSH, "label.menu.item.exit");
			fileExitItem.addSelectionListener(new ExitListener(gui));
		}

		// Help
		final Menu helpMenu = new Menu(shell, SWT.DROP_DOWN);
		final MenuItem helpMenuHeader = newLocalizedMenuItem(bar, SWT.CASCADE, Util.isWindows() ? "label.menu.header.help.windows" : "label.menu.header.help");
		helpMenuHeader.setMenu(helpMenu);

		final MenuItem helpSystemInfoItem = newLocalizedMenuItem(helpMenu, SWT.PUSH, "label.menu.item.system.info");
		helpSystemInfoItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				SystemInformationDialog.open(shell);
			}
		});

		if (!cocoaMenuCreated) {
			new MenuItem(helpMenu, SWT.SEPARATOR);

			final MenuItem helpAboutItem = newLocalizedMenuItem(helpMenu, SWT.PUSH, "label.menu.item.about");
			helpAboutItem.addSelectionListener(new AboutListener(gui));
		}

		final ArmMenuListener helpMenuListener = e -> helpSystemInfoItem.setEnabled(SystemInformationDialog.isAvailable());
		helpMenu.addMenuListener(helpMenuListener);
		helpMenuHeader.addArmListener(helpMenuListener);

		shell.setMenuBar(bar);
	}

	@Getter(AccessLevel.NONE)
	private final LocalizedWidgets localizedWidgets = new LocalizedWidgets();

	@Override
	public void updateLanguage() {
		localizedWidgets.resetAllTexts();
	}

	protected MenuItem newLocalizedMenuItem(@NonNull final Menu parent, final int style, @NonNull final String messageKey) {
		return newLocalizedMenuItem(parent, style, () -> Messages.INSTANCE.get(messageKey));
	}

	protected MenuItem newLocalizedMenuItem(@NonNull final Menu parent, final int style, @NonNull final ISupplier<String> textSupplier) {
		return localizedWidgets.putAndReturn(new MenuItem(parent, style), textSupplier).getKey();
	}

}
