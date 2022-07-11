package io.github.pelmenstar1.digiDict.ui.viewRecord

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.ResourcesMessageMapper

enum class ViewRecordMessage {
    DB_ERROR
}

class ViewRecordMessageMapper(context: Context) :
    ResourcesMessageMapper<ViewRecordMessage>(context, 1) {
    override fun mapToStringResource(type: ViewRecordMessage) = when (type) {
        ViewRecordMessage.DB_ERROR -> R.string.dbError
    }
}