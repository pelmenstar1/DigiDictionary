package io.github.pelmenstar1.digiDict.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent

interface AppWidgetUpdater {
    fun updateAllWidgets()

    private class Impl(
        private val context: Context,
        private val widgetClass: Class<out AppWidgetProvider>
    ) : AppWidgetUpdater {
        private fun createWidgetManualUpdateIntent(ids: IntArray): Intent {
            return Intent().apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
        }

        override fun updateAllWidgets() {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, widgetClass)

            val ids = appWidgetManager.getAppWidgetIds(componentName)
            val intent = createWidgetManualUpdateIntent(ids)

            context.sendBroadcast(intent)
        }
    }

    companion object {
        fun create(context: Context, widgetClass: Class<out AppWidgetProvider>): AppWidgetUpdater {
            return Impl(context, widgetClass)
        }

        inline fun <reified T : AppWidgetProvider> create(context: Context): AppWidgetUpdater {
            return create(context, T::class.java)
        }
    }
}

