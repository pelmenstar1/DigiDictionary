package io.github.pelmenstar1.digiDict.ui.addEditBadge

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.EnumResourcesStringFormatter

enum class AddEditBadgeMessage {
    EMPTY_TEXT,
    NAME_EXISTS
}

class ResourcesAddEditBadgeMessageStringFormatter(
    context: Context
) : EnumResourcesStringFormatter<AddEditBadgeMessage>(context, AddEditBadgeMessage::class.java) {
    override fun getResourceId(value: AddEditBadgeMessage): Int = when (value) {
        AddEditBadgeMessage.EMPTY_TEXT -> R.string.emptyTextError
        AddEditBadgeMessage.NAME_EXISTS -> R.string.addEditBadge_nameExistsError
    }
}