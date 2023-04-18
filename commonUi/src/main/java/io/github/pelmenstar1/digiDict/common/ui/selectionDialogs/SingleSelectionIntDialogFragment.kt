package io.github.pelmenstar1.digiDict.common.ui.selectionDialogs

import android.os.Bundle
import androidx.annotation.StringRes
import kotlin.math.max
import kotlin.math.min

class SingleSelectionIntDialogFragment : SingleSelectionDialogFragment<Int>() {
    private class IntProgressionChoicesProvider(
        private val start: Int,
        private val endInclusive: Int,
        private val step: Int
    ) : ChoicesProvider {
        override fun get(): Array<String> {
            val start = start
            val step = step

            // +1 because 'last' is inclusive.
            val count = (endInclusive - start) / step + 1
            var current = start

            return Array(count) {
                // Note: Integer.toString returns cached String instance if the value is within range (-100; 100).
                // In most cases the method is called with start and endInclusive being within that range.
                val result = current.toString()
                current += step

                result
            }
        }
    }

    private var mutableTitleRes: Int = -1
    private var start: Int = Int.MIN_VALUE
    private var end: Int = Int.MIN_VALUE
    private var step: Int = Int.MIN_VALUE

    override val titleRes: Int
        get() = mutableTitleRes

    override val choices: ChoicesProvider
        get() = IntProgressionChoicesProvider(start, end, step)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireArguments().also { args ->
            mutableTitleRes = args.getInt(ARGS_TITLE_RES)
            start = args.getInt(ARGS_START)
            end = args.getInt(ARGS_END)
            step = args.getInt(ARGS_STEP)
        }
    }

    override fun getValueByIndex(index: Int): Int {
        // Assumes that index lies within choices' array range and onCreate() has been called.
        return start + index * step
    }

    companion object {
        private const val ARGS_TITLE_RES =
            "io.github.pelmenstar1.digiDict.common.ui.SingleSelectionIntDialogFragment.titleRes"
        private const val ARGS_START = "io.github.pelmenstar1.digiDict.common.ui.SingleSelectionIntDialogFragment.start"
        private const val ARGS_END = "io.github.pelmenstar1.digiDict.common.ui.SingleSelectionIntDialogFragment.end"
        private const val ARGS_STEP = "io.github.pelmenstar1.digiDict.common.ui.SingleSelectionIntDialogFragment.step"

        fun createArguments(
            @StringRes titleRes: Int,
            selectedValue: Int,
            start: Int,
            endInclusive: Int,
            step: Int = 1
        ): Bundle {
            return Bundle(5).apply {
                putInt(ARGS_TITLE_RES, titleRes)
                putInt(ARGS_START, start)
                putInt(ARGS_END, endInclusive)
                putInt(ARGS_STEP, step)
                putInt(ARGS_SELECTED_INDEX, getIndexByValue(selectedValue, start, endInclusive, step))
            }
        }

        fun create(
            @StringRes titleRes: Int,
            start: Int,
            endInclusive: Int,
            step: Int = 1
        ): SingleSelectionIntDialogFragment {
            return SingleSelectionIntDialogFragment().apply {
                arguments = createArguments(titleRes, start, endInclusive, step)
            }
        }

        private fun getIndexByValue(value: Int, start: Int, end: Int, step: Int): Int {
            return max(0, (min(end, value) - start) / step)
        }
    }
}