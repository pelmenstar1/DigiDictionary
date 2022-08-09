package io.github.pelmenstar1.digiDict.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context

interface AppWidgetUpdater {
    fun updateAllWidgets()
    fun updateWidgets(ids: IntArray)

    abstract class Base(
        context: Context,
        widgetClass: Class<out AppWidgetProvider>
    ) : AppWidgetUpdater {
        private val appWidgetManager = AppWidgetManager.getInstance(context)
        private val componentName = ComponentName(context, widgetClass)

        override fun updateAllWidgets() {
            val ids = appWidgetManager.getAppWidgetIds(componentName)

            updateWidgets(appWidgetManager, ids)
        }

        override fun updateWidgets(ids: IntArray) {
            updateWidgets(appWidgetManager, ids)
        }

        protected abstract fun updateWidgets(appWidgetManager: AppWidgetManager, ids: IntArray)
    }

    companion object {
        inline fun <reified T : AppWidgetProvider> create(
            context: Context,
            crossinline update: (appWidgetManager: AppWidgetManager, ids: IntArray) -> Unit
        ): AppWidgetUpdater {
            return object : AppWidgetUpdater.Base(context, T::class.java) {
                override fun updateWidgets(appWidgetManager: AppWidgetManager, ids: IntArray) {
                    update(appWidgetManager, ids)
                }
            }
        }
    }
}

