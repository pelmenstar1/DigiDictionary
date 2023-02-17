package io.github.pelmenstar1.digiDict.ui.settings

import android.content.Context
import androidx.annotation.RequiresApi
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.HyphenationFrequency
import io.github.pelmenstar1.digiDict.common.android.ResourcesStringFormatter

@RequiresApi(23)
class ResourcesHyphenationStringFormatter(
    context: Context
) : ResourcesStringFormatter<HyphenationFrequency>(context, 3) {
    // We don't save UNSPECIFIED value here because it doesn't exist
    // That's why valueCount is 3 and we need to -1 ordinal
    override fun getValueOrdinal(value: HyphenationFrequency): Int = value.ordinal - 1

    override fun getResourceId(value: HyphenationFrequency): Int = when (value) {
        HyphenationFrequency.UNSPECIFIED -> throw IllegalStateException("Can't format UNSPECIFIED")
        HyphenationFrequency.NONE -> R.string.settings_hyphenation_none
        HyphenationFrequency.NORMAL -> R.string.settings_hyphenation_normal
        HyphenationFrequency.FULL -> R.string.settings_hyphenation_full
    }
}