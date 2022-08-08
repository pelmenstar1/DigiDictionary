package io.github.pelmenstar1.digiDict.ui.settings

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.resourcesMessageMapper

enum class SettingsMessage {
    EXPORT_ERROR,
    IMPORT_ERROR,
    EXPORT_SUCCESS,
    IMPORT_SUCCESS,
    INVALID_FILE;

    companion object {
        fun defaultMapper(context: Context) = resourcesMessageMapper<SettingsMessage>(context) {
            when (it) {
                EXPORT_ERROR -> R.string.settings_exportError
                IMPORT_ERROR -> R.string.settings_importError
                EXPORT_SUCCESS -> R.string.settings_exportSuccess
                IMPORT_SUCCESS -> R.string.settings_importSuccess
                INVALID_FILE -> R.string.settings_invalidFile
            }
        }
    }
}