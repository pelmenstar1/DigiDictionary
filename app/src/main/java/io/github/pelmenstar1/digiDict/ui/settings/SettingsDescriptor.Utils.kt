package io.github.pelmenstar1.digiDict.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.pelmenstar1.digiDict.common.preferences.AppPreferences
import io.github.pelmenstar1.digiDict.common.ui.settings.SettingsDescriptor
import io.github.pelmenstar1.digiDict.prefs.DigiDictAppPreferences

inline fun <TValue : Any> SettingsDescriptor.ItemGroup.ItemListBuilder.item(
    @StringRes nameRes: Int,
    @DrawableRes iconRes: Int,
    preferenceEntry: DigiDictAppPreferences.Entries.() -> AppPreferences.Entry<TValue, DigiDictAppPreferences.Entries>,
    content: SettingsDescriptor.ItemContentBuilder.() -> SettingsDescriptor.ItemContent<TValue>,
) {
    item(
        nameRes,
        iconRes,
        DigiDictAppPreferences.Entries.preferenceEntry(),
        SettingsDescriptor.ItemContentBuilder.content(),
    )
}