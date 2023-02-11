package io.github.pelmenstar1.digiDict.common.android

/**
 * Represents a functional interface that is used to handle primitive (based on raw fractions) animations.
 */
fun interface AnimationTickCallback {
    /**
     * Called on each animation frame.
     *
     * @param fraction a float that indicates completeness of animation. `0f` indicates the start, `1f` - the end.
     */
    fun onTick(fraction: Float)
}