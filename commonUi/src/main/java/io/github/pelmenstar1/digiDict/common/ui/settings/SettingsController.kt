package io.github.pelmenstar1.digiDict.common.ui.settings

import android.util.SparseArray
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import io.github.pelmenstar1.digiDict.common.StringFormatter
import io.github.pelmenstar1.digiDict.common.preferences.AppPreferences

class SettingsController<TEntries : AppPreferences.Entries> {
    private val actionHandlers = SparseArray<() -> Unit>()
    private val contentItemClickListeners = SparseArray<() -> Unit>()
    private val textFormatters = SparseArray<StringFormatter<Any>>()

    var navController: NavController? = null
    var onValueChangedHandler: ((AppPreferences.Entry<Any, TEntries>, Any) -> Unit)? = null

    fun bindActionHandler(id: Int, handler: () -> Unit) {
        actionHandlers.put(id, handler)
    }

    fun bindContentItemClickListener(id: Int, onClickListener: () -> Unit) {
        if (id == SettingsDescriptor.ITEM_ID_UNSPECIFIED) {
            throw IllegalArgumentException("Cannot bind a listener to an item with unspecified id")
        }

        contentItemClickListeners.put(id, onClickListener)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> bindTextFormatter(id: Int, formatter: StringFormatter<T>) {
        textFormatters.put(id, formatter as StringFormatter<Any>)
    }

    fun <TValue : Any> onValueChanged(entry: AppPreferences.Entry<TValue, TEntries>, newValue: TValue) {
        onValueChangedHandler?.invoke(entry, newValue)
    }

    fun performAction(id: Int) {
        val handler = actionHandlers.get(id)

        handler?.invoke()
    }

    fun performContentItemClickListener(id: Int) {
        val handler = contentItemClickListeners.get(id)

        handler?.invoke()
    }

    fun getTextFormatter(id: Int): StringFormatter<Any>? {
        return textFormatters.get(id)
    }

    fun navigate(directions: NavDirections) {
        navController?.navigate(directions)
    }
}