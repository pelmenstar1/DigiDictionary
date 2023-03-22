package io.github.pelmenstar1.digiDict.ui.startEditEvent

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.EnumResourcesStringFormatter

enum class StartEditEventError {
    EMPTY_TEXT,
    NAME_EXISTS
}

class ResourcesStartEditEventErrorStringFormatter(
    context: Context
) : EnumResourcesStringFormatter<StartEditEventError>(context, StartEditEventError::class.java) {
    override fun getResourceId(value: StartEditEventError): Int = when (value) {
        StartEditEventError.EMPTY_TEXT -> R.string.emptyTextError
        StartEditEventError.NAME_EXISTS -> R.string.startEditEvent_nameExistsError
    }
}