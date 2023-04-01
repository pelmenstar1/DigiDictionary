package io.github.pelmenstar1.digiDict.ui.settings

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import io.github.pelmenstar1.digiDict.common.StringFormatter
import io.github.pelmenstar1.digiDict.common.preferences.AppPreferences
import io.github.pelmenstar1.digiDict.common.ui.selectionDialogs.SingleSelectionIntDialogFragment
import io.github.pelmenstar1.digiDict.common.ui.settings.SettingsController
import io.github.pelmenstar1.digiDict.common.ui.settings.SettingsDescriptor
import io.github.pelmenstar1.digiDict.prefs.DigiDictAppPreferences

typealias DigiDictPrefsGetEntry<TValue> = DigiDictAppPreferences.Entries.() -> AppPreferences.Entry<TValue, DigiDictAppPreferences.Entries>

inline fun <TValue : Any> SettingsDescriptor.ItemGroup.ItemListBuilder<DigiDictAppPreferences.Entries>.item(
    @StringRes nameRes: Int,
    @DrawableRes iconRes: Int? = null,
    preferenceEntry: DigiDictPrefsGetEntry<TValue>,
    clickable: Boolean = false,
    content: SettingsDescriptor.ItemContentBuilder.() -> SettingsDescriptor.ItemContent<TValue>,
) {
    item(
        nameRes,
        iconRes,
        DigiDictAppPreferences.Entries.preferenceEntry(),
        clickable,
        SettingsDescriptor.ItemContentBuilder.content(),
    )
}

inline fun <TValue : Any, reified TDialog : DialogFragment> SettingsDescriptor.DialogListBuilder<DigiDictAppPreferences.Entries>.dialog(
    tag: String,
    entry: DigiDictPrefsGetEntry<TValue>,
    noinline createArgs: ((TValue) -> Bundle)? = null
) {
    dialog<_, TDialog>(tag, DigiDictAppPreferences.Entries.entry(), createArgs)
}

inline fun SettingsDescriptor.DialogListBuilder<DigiDictAppPreferences.Entries>.numberDialog(
    tag: String,
    entry: DigiDictPrefsGetEntry<Int>,
    @StringRes titleRes: Int,
    start: Int,
    endInclusive: Int,
    step: Int = 1
) {
    dialog<_, SingleSelectionIntDialogFragment>(
        tag,
        entry,
        createArgs = { value ->
            SingleSelectionIntDialogFragment.createArguments(
                titleRes,
                value,
                start, endInclusive, step
            )
        }
    )
}

inline fun <TValue : Any> SettingsController<DigiDictAppPreferences.Entries>.bindTextFormatter(
    entry: DigiDictPrefsGetEntry<TValue>,
    formatter: StringFormatter<TValue>
) {
    bindTextFormatter(DigiDictAppPreferences.Entries.entry(), formatter)
}