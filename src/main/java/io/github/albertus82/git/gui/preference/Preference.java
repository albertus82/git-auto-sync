package io.github.albertus82.git.gui.preference;

import static io.github.albertus82.git.gui.preference.PageDefinition.GENERAL;
import static io.github.albertus82.git.gui.preference.PageDefinition.LOGGING;

import java.net.Proxy;
import java.time.ZoneId;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.widgets.Composite;

import io.github.albertus82.git.config.LanguageConfigAccessor;
import io.github.albertus82.git.config.TimeZoneConfigAccessor;
import io.github.albertus82.git.config.logging.LoggingConfig;
import io.github.albertus82.git.config.logging.LoggingLevel;
import io.github.albertus82.git.resources.Messages;
import io.github.albertus82.git.resources.Messages.Language;
import io.github.albertus82.jface.SwtUtils;
import io.github.albertus82.jface.preference.FieldEditorDetails;
import io.github.albertus82.jface.preference.FieldEditorDetails.FieldEditorDetailsBuilder;
import io.github.albertus82.jface.preference.FieldEditorFactory;
import io.github.albertus82.jface.preference.IPreference;
import io.github.albertus82.jface.preference.LocalizedLabelsAndValues;
import io.github.albertus82.jface.preference.PreferenceDetails;
import io.github.albertus82.jface.preference.PreferenceDetails.PreferenceDetailsBuilder;
import io.github.albertus82.jface.preference.StaticLabelsAndValues;
import io.github.albertus82.jface.preference.field.DefaultBooleanFieldEditor;
import io.github.albertus82.jface.preference.field.DefaultComboFieldEditor;
import io.github.albertus82.jface.preference.field.EnhancedDirectoryFieldEditor;
import io.github.albertus82.jface.preference.field.ListFieldEditor;
import io.github.albertus82.jface.preference.field.ScaleIntegerFieldEditor;
import io.github.albertus82.jface.preference.page.IPageDefinition;

public enum Preference implements IPreference {

	LANGUAGE(new PreferenceDetailsBuilder(GENERAL).defaultValue(LanguageConfigAccessor.DEFAULT_LANGUAGE).build(), new FieldEditorDetailsBuilder(DefaultComboFieldEditor.class).labelsAndValues(Preference.getLanguageComboOptions()).build()),
	TIMEZONE(new PreferenceDetailsBuilder(GENERAL).defaultValue(TimeZoneConfigAccessor.DEFAULT_ZONE_ID).build(), new FieldEditorDetailsBuilder(Boolean.TRUE.equals(SwtUtils.isGtk3()) ? ListFieldEditor.class : DefaultComboFieldEditor.class).labelsAndValues(getTimeZoneComboOptions()).height(3).build()), // GTK3 combo rendering is slow when item count is high

	LOGGING_CONSOLE_LEVEL(new PreferenceDetailsBuilder(LOGGING).defaultValue(LoggingConfig.Defaults.LOGGING_LEVEL.toString()).build(), new FieldEditorDetailsBuilder(DefaultComboFieldEditor.class).labelsAndValues(getLoggingComboOptions()).build()),
	LOGGING_FILES_ENABLED(new PreferenceDetailsBuilder(LOGGING).separate().defaultValue(LoggingConfig.Defaults.LOGGING_FILES_ENABLED).build(), new FieldEditorDetailsBuilder(DefaultBooleanFieldEditor.class).build()),
	LOGGING_FILES_LEVEL(new PreferenceDetailsBuilder(LOGGING).parent(LOGGING_FILES_ENABLED).defaultValue(LoggingConfig.Defaults.LOGGING_LEVEL.toString()).build(), new FieldEditorDetailsBuilder(DefaultComboFieldEditor.class).labelsAndValues(getLoggingComboOptions()).build()),
	LOGGING_FILES_PATH(new PreferenceDetailsBuilder(LOGGING).parent(LOGGING_FILES_ENABLED).defaultValue(LoggingConfig.Defaults.LOGGING_FILES_PATH).build(), new FieldEditorDetailsBuilder(EnhancedDirectoryFieldEditor.class).emptyStringAllowed(false).directoryMustExist(false).directoryDialogMessage(() -> Messages.get("message.preferences.directory.dialog.message.log")).build()),
	LOGGING_FILES_LIMIT(new PreferenceDetailsBuilder(LOGGING).parent(LOGGING_FILES_ENABLED).defaultValue(LoggingConfig.Defaults.LOGGING_FILES_MAX_SIZE_KB).build(), new FieldEditorDetailsBuilder(ScaleIntegerFieldEditor.class).scaleMinimum(512).scaleMaximum(8192).scalePageIncrement(512).build()),
	LOGGING_FILES_COUNT(new PreferenceDetailsBuilder(LOGGING).parent(LOGGING_FILES_ENABLED).defaultValue(LoggingConfig.Defaults.LOGGING_FILES_MAX_INDEX).build(), new FieldEditorDetailsBuilder(ScaleIntegerFieldEditor.class).scaleMinimum(1).scaleMaximum(9).scalePageIncrement(1).build()),
	LOGGING_FILES_COMPRESSION_ENABLED(new PreferenceDetailsBuilder(LOGGING).parent(LOGGING_FILES_ENABLED).defaultValue(LoggingConfig.Defaults.LOGGING_FILES_COMPRESSION_ENABLED).build(), new FieldEditorDetailsBuilder(DefaultBooleanFieldEditor.class).build());

	private static final String LABEL_KEY_PREFIX = "label.preferences.";

	private static final FieldEditorFactory fieldEditorFactory = new FieldEditorFactory();

	private final PreferenceDetails preferenceDetails;
	private final FieldEditorDetails fieldEditorDetails;

	Preference(final PreferenceDetails preferenceDetails, final FieldEditorDetails fieldEditorDetails) {
		this.preferenceDetails = preferenceDetails;
		this.fieldEditorDetails = fieldEditorDetails;
		if (preferenceDetails.getName() == null) {
			preferenceDetails.setName(name().toLowerCase(Locale.ROOT).replace('_', '.'));
		}
		if (preferenceDetails.getLabel() == null) {
			preferenceDetails.setLabel(() -> Messages.get(LABEL_KEY_PREFIX + preferenceDetails.getName()));
		}
	}

	@Override
	public String getName() {
		return preferenceDetails.getName();
	}

	@Override
	public String getLabel() {
		return preferenceDetails.getLabel().get();
	}

	@Override
	public IPageDefinition getPageDefinition() {
		return preferenceDetails.getPageDefinition();
	}

	@Override
	public String getDefaultValue() {
		return preferenceDetails.getDefaultValue();
	}

	@Override
	public IPreference getParent() {
		return preferenceDetails.getParent();
	}

	@Override
	public boolean isRestartRequired() {
		return preferenceDetails.isRestartRequired();
	}

	@Override
	public boolean isSeparate() {
		return preferenceDetails.isSeparate();
	}

	@Override
	public Preference[] getChildren() {
		final Set<Preference> preferences = EnumSet.noneOf(Preference.class);
		for (final Preference item : Preference.values()) {
			if (this.equals(item.getParent())) {
				preferences.add(item);
			}
		}
		return preferences.toArray(new Preference[] {});
	}

	@Override
	public FieldEditor createFieldEditor(final Composite parent) {
		return fieldEditorFactory.createFieldEditor(getName(), getLabel(), parent, fieldEditorDetails);
	}

	public static LocalizedLabelsAndValues getLanguageComboOptions() {
		final Language[] values = Messages.Language.values();
		final LocalizedLabelsAndValues options = new LocalizedLabelsAndValues(values.length);
		for (final Language language : values) {
			final Locale locale = language.getLocale();
			final String value = locale.getLanguage();
			options.add(() -> locale.getDisplayLanguage(locale), value);
		}
		return options;
	}

	public static StaticLabelsAndValues getLoggingComboOptions() {
		final StaticLabelsAndValues options = new StaticLabelsAndValues(LoggingLevel.values().length);
		for (final LoggingLevel level : LoggingLevel.values()) {
			options.put(level.toString(), level.toString());
		}
		return options;
	}

	public static StaticLabelsAndValues getTimeZoneComboOptions() {
		final Collection<String> zones = new TreeSet<>(ZoneId.getAvailableZoneIds());
		final StaticLabelsAndValues options = new StaticLabelsAndValues(zones.size());
		for (final String zone : zones) {
			options.put(zone, zone);
		}
		return options;
	}

	public static StaticLabelsAndValues getProxyTypeComboOptions() {
		final Proxy.Type[] types = Proxy.Type.values();
		final StaticLabelsAndValues options = new StaticLabelsAndValues(types.length - 1);
		for (final Proxy.Type type : types) {
			if (!Proxy.Type.DIRECT.equals(type)) {
				options.put(type.toString(), type.name());
			}
		}
		return options;
	}

}
