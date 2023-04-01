package io.github.pelmenstar1.digiDict.common.ui.selectionDialogs

import android.content.Context
import androidx.annotation.ArrayRes

/**
 * Responsible for providing choices for selection dialogs.
 */
fun interface ChoicesProvider {
    fun get(): Array<String>
}

/**
 * Implementation of [ChoicesProvider] that retrieves array with given [resourceId] from resources of specified [context]
 */
class StringArrayResourceChoicesProvider(
    private val context: Context,
    @ArrayRes private val resourceId: Int
) : ChoicesProvider {
    override fun get(): Array<String> {
        return context.resources.getStringArray(resourceId)
    }
}