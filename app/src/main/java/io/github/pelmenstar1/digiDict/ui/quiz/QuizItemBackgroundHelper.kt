package io.github.pelmenstar1.digiDict.ui.quiz

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.annotation.ColorRes
import androidx.core.content.res.ResourcesCompat
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.utils.getLazyValue
import io.github.pelmenstar1.digiDict.utils.transparent
import io.github.pelmenstar1.digiDict.utils.withAlpha

class QuizItemBackgroundHelper(private val context: Context) {
    private var correctAnswerColors: IntArray? = null
    private var wrongAnswerColors: IntArray? = null

    fun getCorrectAnswerBackground(): Drawable {
        val gradientColors = getGradientColors(
            correctAnswerColors,
            R.color.record_quiz_item_correct_answer
        ) { correctAnswerColors = it }

        return createBackground(gradientColors)
    }

    fun getWrongAnswerBackground(): Drawable {
        val gradientColors = getGradientColors(
            wrongAnswerColors,
            R.color.record_quiz_item_wrong_answer
        ) { wrongAnswerColors = it }

        return createBackground(gradientColors)
    }

    private inline fun getGradientColors(
        currentValue: IntArray?,
        @ColorRes colorRes: Int,
        set: (IntArray) -> Unit
    ): IntArray {
        return getLazyValue(
            currentValue,
            {
                val color = ResourcesCompat.getColor(context.resources, colorRes, context.theme)

                intArrayOf(
                    color.withAlpha(127),
                    color.transparent()
                )
            },
            set
        )
    }

    private fun createBackground(gradientColors: IntArray): Drawable {
        return GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradientColors)
    }
}