package io.github.pelmenstar1.digiDict.common.ui.settings

import io.github.pelmenstar1.digiDict.common.preferences.AppPreferences

fun interface SettingsContentItemClickAction<TValue : Any, TEntries : AppPreferences.Entries> {
    fun perform(
        entry: AppPreferences.Entry<TValue, TEntries>,
        controller: SettingsController<TEntries>
    )
}

object SettingsContentItemClickActions {
    fun <TValue : Any, TEntries : AppPreferences.Entries> showDialog() =
        SettingsContentItemClickAction<TValue, TEntries> { entry, controller ->
            controller.showDialogForEntry(entry)
        }
}