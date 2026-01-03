package io.github.albertus82.git.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import io.github.albertus82.git.config.ApplicationConfig;
import io.github.albertus82.git.gui.preference.Preference;
import io.github.albertus82.git.resources.Messages;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public class CloseDialog {

	private static final Messages messages = Messages.INSTANCE;

	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static class Defaults {
		public static final boolean CONFIRM_CLOSE = false;
	}

	private final MessageBox messageBox;

	private CloseDialog(final Shell shell) {
		messageBox = new MessageBox(shell, SWT.YES | SWT.NO | SWT.ICON_QUESTION);
		messageBox.setText(messages.get("message.confirm.close.text"));
		messageBox.setMessage(messages.get("message.confirm.close.message"));
	}

	public static int open(final Shell shell) {
		return new CloseDialog(shell).messageBox.open();
	}

	public static boolean mustShow() {
		return ApplicationConfig.getPreferencesConfiguration().getBoolean(Preference.CONFIRM_CLOSE, Defaults.CONFIRM_CLOSE);
	}

}
