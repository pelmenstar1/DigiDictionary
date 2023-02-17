package io.github.pelmenstar1.digiDict.ui.settings

import android.content.Context
import androidx.annotation.RequiresApi
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.BreakStrategy
import io.github.pelmenstar1.digiDict.common.android.ResourcesStringFormatter

@RequiresApi(23)
class ResourcesBreakStrategyStringFormatter(
    context: Context
) : ResourcesStringFormatter<BreakStrategy>(context, 3) {
    // We don't save UNSPECIFIED value here because it doesn't exist
    // That's why valueCount is 3 and we need to -1 ordinal
    override fun getValueOrdinal(value: BreakStrategy): Int = value.ordinal - 1

    override fun getResourceId(value: BreakStrategy): Int = when (value) {
        BreakStrategy.UNSPECIFIED -> throw IllegalStateException("Can't format UNSPECIFIED")
        BreakStrategy.SIMPLE -> R.string.settings_breakStrategy_simple
        BreakStrategy.BALANCED -> R.string.settings_breakStrategy_balanced
        BreakStrategy.HIGH_QUALITY -> R.string.settings_breakStrategy_highQuality
    }
}