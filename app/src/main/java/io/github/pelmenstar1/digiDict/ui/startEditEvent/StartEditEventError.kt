package io.github.pelmenstar1.digiDict.ui.startEditEvent

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.resourcesMessageMapper

enum class StartEditEventError {
    EMPTY_TEXT,
    NAME_EXISTS;

    companion object {
        fun resourcesMapper(context: Context) = resourcesMessageMapper<StartEditEventError>(context) {
            when (it) {
                EMPTY_TEXT -> R.string.emptyTextError
                NAME_EXISTS -> R.string.startEditEvent_nameExistsError
            }
        }
    }
}