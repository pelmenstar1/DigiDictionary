package io.github.pelmenstar1.digiDict.ui.settings

import android.content.res.Resources
import androidx.annotation.RequiresApi
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.CachedResourcesStringFormatter
import io.github.pelmenstar1.digiDict.common.android.HyphenationFrequency

@RequiresApi(23)
class ResourcesHyphenationStringFormatter(
    resources: Resources
) : CachedResourcesStringFormatter<HyphenationFrequency>(resources, 3) {
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