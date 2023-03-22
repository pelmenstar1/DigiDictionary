package io.github.pelmenstar1.digiDict.common.android

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Provides a single getter [flow] that returns the flow of [TextBreakAndHyphenationInfo].
 *
 * That's done to better support dependency injection as the flow can be of any nature.
 */
interface TextBreakAndHyphenationInfoSource {
    val flow: Flow<TextBreakAndHyphenationInfo>
}

/**
 * An implementation of [TextBreakAndHyphenationInfoSource] whose [TextBreakAndHyphenationInfoSource.flow] always emits
 * [TextBreakAndHyphenationInfo.UNSPECIFIED]
 */
object NoOpTextBreakAndHyphenationInfoSource : TextBreakAndHyphenationInfoSource {
    override val flow: Flow<TextBreakAndHyphenationInfo> = flowOf(TextBreakAndHyphenationInfo.UNSPECIFIED)
}