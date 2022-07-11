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
        SettingsMessage.EXPORT_ERROR -> R.string.exportError
        SettingsMessage.IMPORT_ERROR -> R.string.importError
        SettingsMessage.EXPORT_SUCCESS -> R.string.exportSuccess
        SettingsMessage.IMPORT_SUCCESS -> R.string.importSuccess
        SettingsMessage.INVALID_FILE -> R.string.invalidFile
    }
}