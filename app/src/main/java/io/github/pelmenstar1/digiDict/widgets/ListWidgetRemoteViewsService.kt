package io.github.pelmenstar1.digiDict.widgets

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import io.github.pelmenstar1.digiDict.prefs.get
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class ListWidgetRemoteViewsService : RemoteViewsService() {
    @Inject
    lateinit var recordDao: RecordDao

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory {
        return ListWidgetRemoteViewsFactory(applicationContext, recordDao, appPreferences)
    }
}

class ListWidgetRemoteViewsFactory(
    private val context: Context,
    private val recordDao: RecordDao,
    private val appPreferences: AppPreferences
) : RemoteViewsService.RemoteViewsFactory {
    private var records: Array<RecordDao.IdExpressionMeaningRecord>? = null

    override fun onCreate() {
    }

    override fun onDestroy() {
    }

    override fun onDataSetChanged() {
        val n = runBlocking {
            appPreferences.get { widgetListMaxSize }
        }

        records = recordDao.getLastIdExprMeaningRecordsBlocking(n)
    }

    override fun getCount() = records?.size ?: 0

    override fun getViewAt(position: Int): RemoteViews {
        val record = records?.get(position) ?: throw IllegalStateException("Records are not loaded")

        return RemoteViews(context.packageName, R.layout.widget_list_item).also {
            it.setTextViewText(
                R.id.listWidget_item_expression,
                record.expression
            )
            it.setTextViewText(
                R.id.listWidget_item_meaning,
                MeaningTextHelper.parseToFormattedAndHandleErrors(context, record.meaning)
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