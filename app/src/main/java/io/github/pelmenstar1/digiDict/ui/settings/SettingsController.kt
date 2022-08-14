package io.github.pelmenstar1.digiDict.ui.settings

import android.util.SparseArray
import io.github.pelmenstar1.digiDict.prefs.AppPreferences

class SettingsController {
    private val actionHandlers = SparseArray<() -> Unit>()

    var onValueChangedHandler: ((AppPreferences.Entry<Any>, Any) -> Unit)? = null

    fun bindActionHandler(id: Int, handler: () -> Unit) {
        actionHandlers.put(id, handler)
    }

    fun <T : Any> onValueChanged(entry: AppPreferences.Entry<T>, newValue: T) {
        onValueChangedHandler?.invoke(entry, newValue)
    }

    fun performAction(id: Int) {
        val handler = actionHandlers.get(id)

        handler?.invoke()
    }
}