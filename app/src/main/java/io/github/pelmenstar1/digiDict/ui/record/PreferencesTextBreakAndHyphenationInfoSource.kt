package io.github.pelmenstar1.digiDict.ui.record

import androidx.annotation.RequiresApi
import io.github.pelmenstar1.digiDict.common.android.TextBreakAndHyphenationInfo
import io.github.pelmenstar1.digiDict.common.android.TextBreakAndHyphenationInfoSource
import io.github.pelmenstar1.digiDict.prefs.DigiDictAppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * An implementation of [TextBreakAndHyphenationInfoSource] that extracts the required information from [DigiDictAppPreferences].
 */
@RequiresApi(23)
class PreferencesTextBreakAndHyphenationInfoSource @Inject constructor(
    prefs: DigiDictAppPreferences
) : TextBreakAndHyphenationInfoSource {
    override val flow: Flow<TextBreakAndHyphenationInfo> = prefs.getSnapshotFlow {
        arrayOf(recordTextBreakStrategy, recordTextHyphenationFrequency)
    }.map { snapshot ->
        val breakStrategy = snapshot.recordTextBreakStrategy
        val hyphenationFrequency = snapshot.recordTextHyphenationFrequency

        TextBreakAndHyphenationInfo(breakStrategy, hyphenationFrequency)
    }
}