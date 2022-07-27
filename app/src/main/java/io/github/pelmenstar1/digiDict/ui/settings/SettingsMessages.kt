package io.github.pelmenstar1.digiDict.ui.settings

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.ResourcesMessageMapper

enum class SettingsMessage {
    EXPORT_ERROR,
    IMPORT_ERROR,
    EXPORT_SUCCESS,
    IMPORT_SUCCESS,
    INVALID_FILE
}

class SettingsMessageMapper(context: Context) :
    ResourcesMessageMapper<SettingsMessage>(context, 5) {
    override fun mapToStringResource(type: SettingsMessage) = when (type) {
        SettingsMessage.EXPORT_ERROR -> R.string.settings_exportError
        SettingsMessage.IMPORT_ERROR -> R.string.settings_importError
        SettingsMessage.EXPORT_SUCCESS -> R.string.settings_exportSuccess
        SettingsMessage.IMPORT_SUCCESS -> R.string.settings_importSuccess
        SettingsMessage.INVALID_FILE -> R.string.settings_invalidFile
    }
}