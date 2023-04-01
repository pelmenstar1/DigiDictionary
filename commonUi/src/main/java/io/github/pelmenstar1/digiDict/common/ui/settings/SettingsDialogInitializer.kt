package io.github.pelmenstar1.digiDict.common.ui.settings

import androidx.fragment.app.DialogFragment

fun interface SettingsDialogInitializer<in TValue : Any, TDialog : DialogFragment> {
    fun init(dialog: TDialog, propertyValue: TValue)
}