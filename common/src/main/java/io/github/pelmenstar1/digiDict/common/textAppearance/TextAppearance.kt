package io.github.pelmenstar1.digiDict.common.textAppearance

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.LocaleList
import android.util.Log
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.annotation.StyleableRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat
import java.util.*

/**
 * Provides a way to save text appearance style data and apply it to [TextView]. There can be some minor inconsistencies when using
 * this class and [TextView.setTextAppearance]
 */
@SuppressLint("PrivateResource")
class TextAppearance(context: Context, @StyleRes styleRes: Int) {
    private val textColor: ColorStateList?
    private val textColorHint: ColorStateList?
    private val textColorLink: ColorStateList?

    private val allCaps: AllCapsTransformationMethod?

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

    private val textLocale: Locale?
    private val textLocaleList: LocaleList?

    init {
        val theme = context.theme

        val a = theme.obtainStyledAttributes(styleRes, com.google.android.material.R.styleable.TextAppearance)

        textSize = a.getDimension(androidx.appcompat.R.styleable.TextAppearance_android_textSize, 0f)
        textColor = a.getColorStateList(androidx.appcompat.R.styleable.TextAppearance_android_textColor)
        textColorHint = a.getColorStateList(androidx.appcompat.R.styleable.TextAppearance_android_textColorHint)
        textColorLink = a.getColorStateList(androidx.appcompat.R.styleable.TextAppearance_android_textColorLink)

        allCaps = if (a.getBoolean(androidx.appcompat.R.styleable.TextAppearance_textAllCaps, false)) {
            AllCapsTransformationMethod(context)
        } else {
            null
        }

        shadowColor =
            a.getColor(androidx.appcompat.R.styleable.TextAppearance_android_shadowColor, Color.TRANSPARENT)
        shadowDx = a.getFloat(androidx.appcompat.R.styleable.TextAppearance_android_shadowDx, 0f)
        shadowDy = a.getFloat(androidx.appcompat.R.styleable.TextAppearance_android_shadowDy, 0f)
        shadowRadius = a.getFloat(androidx.appcompat.R.styleable.TextAppearance_android_shadowRadius, 0f)

        val materialAttrs =
            theme.obtainStyledAttributes(styleRes, com.google.android.material.R.styleable.MaterialTextAppearance)
        try {
            letterSpacing = materialAttrs.getFloat(
                com.google.android.material.R.styleable.MaterialTextAppearance_android_letterSpacing,
                Float.NaN
            )
        } finally {
            materialAttrs.recycle()
        }

        this.font = resolveFont(context, a)

        lineHeight = if (canApplyTextAppearanceLineHeight(context)) {
            getLineHeight(context, styleRes)
        } else {
            -1
        }

        fontVariationSettings = if (Build.VERSION.SDK_INT >= 26) {
            a.getString(androidx.appcompat.R.styleable.TextAppearance_fontVariationSettings)
        } else {
            null
        }

        val localeStr = a.getString(androidx.appcompat.R.styleable.TextAppearance_textLocale)
        if (localeStr != null) {
            if (Build.VERSION.SDK_INT >= 24) {
                textLocale = null
                textLocaleList = LocaleList.forLanguageTags(localeStr)
            } else {
                textLocale = Locale.forLanguageTag(localeStr)
                textLocaleList = null
            }
        } else {
            textLocale = null
            textLocaleList = null
        }
    }

    fun apply(textView: TextView) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)

        textColor?.also(textView::setTextColor)
        textColorHint?.also(textView::setHintTextColor)
        textColorLink?.also(textView::setLinkTextColor)
        allCaps?.let(textView::setTransformationMethod)

        textView.typeface = font

        if (shadowColor != Color.TRANSPARENT) {
            textView.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        }

        if (!letterSpacing.isNaN()) {
            textView.letterSpacing = letterSpacing
        }

        if (lineHeight >= 0) {
            TextViewCompat.setLineHeight(textView, lineHeight)
        }

        if (Build.VERSION.SDK_INT >= 26) {
            fontVariationSettings?.also(textView::setFontVariationSettings)
        }

        if (Build.VERSION.SDK_INT >= 24) {
            textLocaleList?.also(textView::setTextLocales)
        } else {
            textLocale?.also(textView::setTextLocale)
        }
    }

    companion object {
        private const val TAG = "TextAppearance"

        // Enums from AppCompatTextHelper.
        private const val TYPEFACE_SANS = 1
        private const val TYPEFACE_SERIF = 2
        private const val TYPEFACE_MONOSPACE = 3

        internal fun canApplyTextAppearanceLineHeight(context: Context): Boolean {
            val value = TypedValue()
            val resolved = context.theme.resolveAttribute(
                com.google.android.material.R.attr.textAppearanceLineHeightEnabled,
                value,
                true
            )

            return if (resolved && value.type == TypedValue.TYPE_INT_BOOLEAN) {
                value.data != 0
            } else {
                true
            }
        }

        internal fun getLineHeight(context: Context, @StyleRes styleRes: Int): Int {
            val attrs = context.theme.obtainStyledAttributes(
                styleRes,
                com.google.android.material.R.styleable.MaterialTextAppearance
            )

            var lineHeight = getDimensionPixelSize(
                context,
                attrs,
                com.google.android.material.R.styleable.MaterialTextAppearance_android_lineHeight,
                -1
            )

            if (lineHeight < 0) {
                lineHeight = getDimensionPixelSize(
                    context,
                    attrs,
                    com.google.android.material.R.styleable.MaterialTextAppearance_lineHeight,
                    -1
                )
            }

            return lineHeight
        }

        internal fun resolveFont(context: Context, a: TypedArray): Typeface {
            val fontFamilyIndex = if (a.hasValue(androidx.appcompat.R.styleable.TextAppearance_fontFamily))
                androidx.appcompat.R.styleable.TextAppearance_fontFamily
            else
                androidx.appcompat.R.styleable.TextAppearance_android_fontFamily

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
                    Log.e(TAG, "Error loading font $fontFamily", e)
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
                )
            }

            return font!!
        }

        fun getDimensionPixelSize(
            context: Context,
            attributes: TypedArray,
            @StyleableRes index: Int,
            defaultValue: Int
        ): Int {
            val value = TypedValue()
            if (!attributes.getValue(index, value) || value.type != TypedValue.TYPE_ATTRIBUTE) {
                return attributes.getDimensionPixelSize(index, defaultValue)
            }

            val styledAttrs = context.theme.obtainStyledAttributes(intArrayOf(value.data))
            val dimension = styledAttrs.getDimensionPixelSize(0, defaultValue)
            styledAttrs.recycle()

            return dimension
        }
    }
}