package io.github.pelmenstar1.digiDict.common.textAppearance

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.LocaleList
import android.text.TextPaint
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.annotation.StyleableRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat
import java.util.*
import kotlin.math.min

/**
 * Provides a way to save text appearance style data and apply it to [TextView]. There can be some minor inconsistencies when using
 * this class and [TextView.setTextAppearance]
 */
@SuppressLint("PrivateResource")
class TextAppearance(context: Context, @StyleRes styleRes: Int) {
    private val textColor: ColorStateList?
    private val textColorHint: ColorStateList?
    private val textColorLink: ColorStateList?

    private val isAllCaps: Boolean

    @ColorInt
    private val shadowColor: Int
    private val shadowDx: Float
    private val shadowDy: Float
    private val shadowRadius: Float
    private val letterSpacing: Float
    private val textSize: Float
    private val font: Typeface
    private val fontVariationSettings: String?

    private val lineHeight: Int

    // If API level >= 24, the type is LocaleList, otherwise Locale.
    // If there's no locale set, it's null.
    private val textLocaleOrList: Any?

    init {
        val theme = context.theme

        val a = theme.obtainStyledAttributes(styleRes, com.google.android.material.R.styleable.TextAppearance)

        try {
            textSize = a.getDimension(androidx.appcompat.R.styleable.TextAppearance_android_textSize, 0f)
            textColor = a.getColorStateList(androidx.appcompat.R.styleable.TextAppearance_android_textColor)
            textColorHint = a.getColorStateList(androidx.appcompat.R.styleable.TextAppearance_android_textColorHint)
            textColorLink = a.getColorStateList(androidx.appcompat.R.styleable.TextAppearance_android_textColorLink)

            isAllCaps = a.getBoolean(androidx.appcompat.R.styleable.TextAppearance_textAllCaps, false)

            shadowColor =
                a.getColor(androidx.appcompat.R.styleable.TextAppearance_android_shadowColor, Color.TRANSPARENT)

            if (shadowColor != Color.TRANSPARENT) {
                shadowDx = a.getFloat(androidx.appcompat.R.styleable.TextAppearance_android_shadowDx, 0f)
                shadowDy = a.getFloat(androidx.appcompat.R.styleable.TextAppearance_android_shadowDy, 0f)
                shadowRadius = a.getFloat(androidx.appcompat.R.styleable.TextAppearance_android_shadowRadius, 0f)
            } else {
                shadowDx = 0f
                shadowDy = 0f
                shadowRadius = 0f
            }

            letterSpacing = getLetterSpacing(theme, styleRes)
            font = resolveFont(context, a)
            lineHeight = getLineHeightIfCanApply(theme, styleRes)

            fontVariationSettings = if (Build.VERSION.SDK_INT >= 26) {
                a.getString(androidx.appcompat.R.styleable.TextAppearance_fontVariationSettings)
            } else {
                null
            }

            textLocaleOrList = getLocale(a)
        } finally {
            a.recycle()
        }
    }

    fun apply(textView: TextView) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)

        textColor?.also(textView::setTextColor)
        textColorHint?.also(textView::setHintTextColor)
        textColorLink?.also(textView::setLinkTextColor)

        textView.isAllCaps = isAllCaps
        textView.typeface = font

        val shadowColor = shadowColor
        if (shadowColor != Color.TRANSPARENT) {
            textView.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        }

        letterSpacing.also {
            if (!it.isNaN()) {
                textView.letterSpacing = it
            }
        }

        lineHeight.also {
            if (it >= 0) {
                TextViewCompat.setLineHeight(textView, it)
            }
        }

        if (Build.VERSION.SDK_INT >= 26) {
            fontVariationSettings?.also(textView::setFontVariationSettings)
        }

        useLocaleOrList(
            onLocale = { textView.textLocale = it },
            onLocaleList = { textView.textLocales = it }
        )
    }

    fun getTextPaintForMeasure(): TextPaint {
        val paint = TextPaint()

        paint.textSize = textSize
        paint.typeface = font

        letterSpacing.also {
            if (!it.isNaN()) {
                paint.letterSpacing = it
            }
        }

        if (Build.VERSION.SDK_INT >= 26) {
            fontVariationSettings?.also(paint::setFontVariationSettings)
        }

        useLocaleOrList(
            onLocale = { paint.textLocale = it },
            onLocaleList = { paint.textLocales = it }
        )

        return paint
    }

    @ChecksSdkIntAtLeast(api = 24, lambda = 1)
    private inline fun useLocaleOrList(onLocale: (Locale) -> Unit, onLocaleList: (LocaleList) -> Unit) {
        val obj = textLocaleOrList

        if (obj != null) {
            if (Build.VERSION.SDK_INT >= 24) {
                onLocaleList(obj as LocaleList)
            } else {
                onLocale(obj as Locale)
            }
        }
    }

    companion object {
        // Enums from AppCompatTextHelper.
        private const val TYPEFACE_SANS = 1
        private const val TYPEFACE_SERIF = 2
        private const val TYPEFACE_MONOSPACE = 3

        internal fun getLocale(a: TypedArray): Any? {
            val localeStr = a.getString(androidx.appcompat.R.styleable.TextAppearance_textLocale)

            return if (localeStr != null) {
                if (Build.VERSION.SDK_INT >= 24) {
                    LocaleList.forLanguageTags(localeStr)
                } else {
                    Locale.forLanguageTag(localeStr)
                }
            } else {
                null
            }
        }

        internal fun getLetterSpacing(theme: Resources.Theme, styleRes: Int): Float {
            val materialAttrs =
                theme.obtainStyledAttributes(styleRes, com.google.android.material.R.styleable.MaterialTextAppearance)

            try {
                return materialAttrs.getFloat(
                    com.google.android.material.R.styleable.MaterialTextAppearance_android_letterSpacing,
                    Float.NaN
                )
            } finally {
                materialAttrs.recycle()
            }
        }

        private fun canApplyTextAppearanceLineHeight(theme: Resources.Theme, tempValue: TypedValue): Boolean {
            val resolved = theme.resolveAttribute(
                com.google.android.material.R.attr.textAppearanceLineHeightEnabled,
                tempValue,
                true
            )

            return if (resolved && tempValue.type == TypedValue.TYPE_INT_BOOLEAN) {
                tempValue.data != 0
            } else {
                true
            }
        }

        private fun getLineHeight(theme: Resources.Theme, @StyleRes styleRes: Int, tempValue: TypedValue): Int {
            val attrs = theme.obtainStyledAttributes(
                styleRes,
                com.google.android.material.R.styleable.MaterialTextAppearance
            )

            var lineHeight = getDimensionPixelSize(
                theme,
                attrs,
                com.google.android.material.R.styleable.MaterialTextAppearance_android_lineHeight,
                tempValue
            )

            if (lineHeight < 0) {
                lineHeight = getDimensionPixelSize(
                    theme,
                    attrs,
                    com.google.android.material.R.styleable.MaterialTextAppearance_lineHeight,
                    tempValue
                )
            }

            return lineHeight
        }

        internal fun getLineHeightIfCanApply(theme: Resources.Theme, @StyleRes styleRes: Int): Int {
            val tempValue = TypedValue()

            return if (canApplyTextAppearanceLineHeight(theme, tempValue)) {
                getLineHeight(theme, styleRes, tempValue)
            } else {
                -1
            }
        }

        internal fun resolveFont(context: Context, a: TypedArray): Typeface {
            val fontFamilyIndex = if (a.hasValue(androidx.appcompat.R.styleable.TextAppearance_fontFamily)) {
                androidx.appcompat.R.styleable.TextAppearance_fontFamily
            } else {
                androidx.appcompat.R.styleable.TextAppearance_android_fontFamily
            }

            val fontFamilyResourceId = a.getResourceId(fontFamilyIndex, 0)
            val fontFamily = a.getString(fontFamilyIndex)

            val textStyle = a.getInt(androidx.appcompat.R.styleable.TextAppearance_android_textStyle, Typeface.NORMAL)

            var font: Typeface? = null
            if (fontFamilyResourceId != 0) {
                try {
                    val family = ResourcesCompat.getFont(context, fontFamilyResourceId)

                    if (family != null) {
                        font = Typeface.create(family, textStyle)
                    }
                } catch (e: Exception) {
                    // Expected when the resource is not found.
                }
            }

            if (font == null && fontFamily != null) {
                font = Typeface.create(fontFamily, textStyle)
            }

            // Try resolving typeface if specified otherwise fallback to Typeface.DEFAULT.
            if (font == null) {
                val typeface = a.getInt(androidx.appcompat.R.styleable.TextAppearance_android_typeface, TYPEFACE_SANS)

                font = Typeface.create(
                    when (typeface) {
                        TYPEFACE_SANS -> Typeface.SANS_SERIF
                        TYPEFACE_SERIF -> Typeface.SERIF
                        TYPEFACE_MONOSPACE -> Typeface.MONOSPACE
                        else -> Typeface.DEFAULT
                    }, textStyle
                )!!
            }

            if (Build.VERSION.SDK_INT >= 28) {
                val italic = (textStyle and Typeface.ITALIC) != 0
                var weight = a.getInt(androidx.appcompat.R.styleable.TextAppearance_android_textFontWeight, -1)

                if (weight >= 0) {
                    // 1000 is max value of weight. Force it.
                    weight = min(1000, weight)

                    font = Typeface.create(font, weight, italic)
                }
            }

            return font
        }

        private fun getDimensionPixelSize(
            theme: Resources.Theme,
            attributes: TypedArray,
            @StyleableRes index: Int,
            tempValue: TypedValue
        ): Int {
            if (!attributes.getValue(index, tempValue) || tempValue.type != TypedValue.TYPE_ATTRIBUTE) {
                return attributes.getDimensionPixelSize(index, -1)
            }

            val styledAttrs = theme.obtainStyledAttributes(intArrayOf(tempValue.data))

            try {
                return styledAttrs.getDimensionPixelSize(0, -1)
            } finally {
                styledAttrs.recycle()
            }
        }
    }
}

inline fun TextAppearance(context: Context, select: MaterialTextAppearanceSelector.() -> Int): TextAppearance {
    return TextAppearance(context, MaterialTextAppearanceSelector.select())
}