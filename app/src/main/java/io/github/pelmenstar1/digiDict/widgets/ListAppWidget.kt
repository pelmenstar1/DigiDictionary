package io.github.pelmenstar1.digiDict.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import io.github.pelmenstar1.digiDict.R

class ListAppWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updater(context).updateWidgets(appWidgetIds)
    }

    companion object {
        fun updater(context: Context) = AppWidgetUpdater.create<ListAppWidget>(context) { appWidgetManager, ids ->
            val intent = Intent(context, ListWidgetRemoteViewsService::class.java)

            val rv = RemoteViews(context.packageName, R.layout.widget_list).also {
                it.setRemoteAdapter(R.id.listWidget_list, intent)
            }

            appWidgetManager.updateAppWidget(ids, rv)
            appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.listWidget_list)
        }
    }
}

