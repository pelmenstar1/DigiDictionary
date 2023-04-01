package io.github.pelmenstar1.digiDict.common.ui.selectionDialogs

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import io.github.pelmenstar1.digiDict.common.android.MaterialDialogFragment

abstract class AbstractSelectionDialogFragment : MaterialDialogFragment() {
    /**
     * Gets a string resource that stores a title of the dialog
     */
    @get:StringRes
    protected abstract val titleRes: Int

    /**
     * Gets a provider of choices.
     */
    protected abstract val choices: ChoicesProvider

    /**
     * A shortcut for creating [StringArrayResourceChoicesProvider]
     */
    protected fun stringArrayResource(@ArrayRes id: Int): ChoicesProvider {
        return StringArrayResourceChoicesProvider(requireContext(), id)
    }
}