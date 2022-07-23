package io.github.pelmenstar1.digiDict.widgets

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.SECONDS_IN_DAY
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper

class ListWidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent?) =
        ListWidgetRemoteViewsFactory(applicationContext)
}

class ListWidgetRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {
    private var records: Array<Record>? = null
    private val dao = AppDatabase.getOrCreate(context).recordDao()

    override fun onCreate() {
    }

    override fun onDestroy() {
    }

    override fun onDataSetChanged() {
        val nowEpochSecondsUtc = System.currentTimeMillis() / 1000

        records = dao.getRecordsAfterBlocking(nowEpochSecondsUtc - SECONDS_IN_DAY)
    }

    override fun getCount() = records?.size ?: 0

    override fun getViewAt(position: Int): RemoteViews {
        val record = records?.get(position) ?: throw IllegalStateException("Records are not loaded")

        return RemoteViews(context.packageName, R.layout.widget_list_item).also {
            it.setTextViewText(R.id.listWidget_item_expression, record.expression)
            it.setTextViewText(
                R.id.listWidget_item_meaning,
                MeaningTextHelper.parseToFormatted(record.rawMeaning)
            )
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount() = 1
    override fun hasStableIds() = true

    override fun getItemId(position: Int): Long {
        return records?.let { it[position].id.toLong() } ?: 0L
    }
}