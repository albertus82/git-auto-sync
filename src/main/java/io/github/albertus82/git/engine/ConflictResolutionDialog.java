package io.github.albertus82.git.engine;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

public final class ConflictResolutionDialog {

	private ConflictResolutionDialog() {}

	public static ConflictChoice ask(Shell parent, String filePath) {

		final String[] buttons = { "Keep OURS", "Keep THEIRS", "Keep BOTH" };

		final var dialog = new MessageDialog(parent, "Merge Conflict", null, "Conflict detected in:\n\n" + filePath + "\n\nChoose how to resolve:", MessageDialog.QUESTION, buttons, 0);

		final var result = dialog.open();

		return switch (result) {
		case 0 -> ConflictChoice.OURS;
		case 1 -> ConflictChoice.THEIRS;
		case 2 -> ConflictChoice.BOTH;
		default -> throw new IllegalStateException("Dialog closed");
		};
	}

}
