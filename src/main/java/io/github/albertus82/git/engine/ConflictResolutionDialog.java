package io.github.albertus82.git.engine;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

public final class ConflictResolutionDialog {

    private ConflictResolutionDialog() {}

    public static ConflictResolution ask(Shell parent, String filePath) {

        String[] buttons = {
                "Keep OURS",
                "Keep THEIRS",
                "Keep BOTH"
        };

        MessageDialog dialog = new MessageDialog(
                parent,
                "Merge Conflict",
                null,
                "Conflict detected in:\n\n" + filePath +
                "\n\nChoose how to resolve:",
                MessageDialog.QUESTION,
                buttons,
                0
        );

        int result = dialog.open();

        return switch (result) {
            case 0 -> ConflictResolution.OURS;
            case 1 -> ConflictResolution.THEIRS;
            case 2 -> ConflictResolution.BOTH;
            default -> throw new IllegalStateException("Dialog closed");
        };
    }
}
