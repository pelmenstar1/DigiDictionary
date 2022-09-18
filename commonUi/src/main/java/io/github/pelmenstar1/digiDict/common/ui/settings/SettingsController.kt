package io.github.pelmenstar1.digiDict.common.ui.settings

import android.util.SparseArray
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import io.github.pelmenstar1.digiDict.common.preferences.AppPreferences

class SettingsController<TEntries : AppPreferences.Entries> {
    private val actionHandlers = SparseArray<() -> Unit>()

    var navController: NavController? = null
    var onValueChangedHandler: ((AppPreferences.Entry<Any, TEntries>, Any) -> Unit)? = null

    fun bindActionHandler(id: Int, handler: () -> Unit) {
        actionHandlers.put(id, handler)
    }

    fun <TValue : Any> onValueChanged(entry: AppPreferences.Entry<TValue, TEntries>, newValue: TValue) {
        onValueChangedHandler?.invoke(entry, newValue)
    }

    fun performAction(id: Int) {
        val handler = actionHandlers.get(id)

        handler?.invoke()
    }

    fun navigate(directions: NavDirections) {
        navController?.navigate(directions)
    }
}