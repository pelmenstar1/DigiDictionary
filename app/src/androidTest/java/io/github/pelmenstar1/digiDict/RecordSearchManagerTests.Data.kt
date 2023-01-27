package io.github.pelmenstar1.digiDict

import androidx.test.platform.app.InstrumentationRegistry

object RecordSearchManagerTestsData {
    private const val WORD_COUNT = 10_000

    val words: Array<String>

    init {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val res = context.resources

        res.openRawResource(R.raw.words).use {
            it.bufferedReader().also { reader ->
                words = Array(WORD_COUNT) { reader.readLine() }
            }
        }
    }
}