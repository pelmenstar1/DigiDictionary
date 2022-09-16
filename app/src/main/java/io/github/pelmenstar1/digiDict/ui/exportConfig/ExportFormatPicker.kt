package io.github.pelmenstar1.digiDict.ui.exportConfig

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View.OnClickListener
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat
import io.github.pelmenstar1.digiDict.backup.BackupFormat
import io.github.pelmenstar1.digiDict.common.ui.getTypedViewAt
import io.github.pelmenstar1.digiDict.common.unsafeNewArray

class ExportFormatPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private var formats = emptyArray<BackupFormat>()
    private val itemOnClickListener = OnClickListener {
        selectedIndex = it.tag as Int
    }

    var selectedIndex: Int = 0
        set(index) {
            field = index

            for (i in 0 until childCount) {
                getEntryViewAt(i).isChecked = i == index
            }

            onItemSelected?.invoke(selectedFormat)
        }

    var onItemSelected: ((BackupFormat) -> Unit)? = null

    val selectedFormat: BackupFormat
        get() = formats[selectedIndex]

    init {
        orientation = VERTICAL
    }

    fun setItems(entries: Array<out ExportFormatEntry>) {
        // In case, setEntries() has been already called
        removeAllViews()

        val context = context
        val res = context.resources
        val theme = context.theme
        val selectableItemBackground = getSelectableItemBackground(context)

        val formats = unsafeNewArray<BackupFormat>(entries.size)

        entries.forEachIndexed { index, (format, descId) ->
            formats[index] = format

            addView(ExportFormatEntryView(context).also {
                it.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                it.name = format.shortName
                it.description = res.getText(descId)
                it.background = selectableItemBackground?.constantState?.newDrawable(res, theme)
                it.tag = index
                it.setOnClickListener(itemOnClickListener)
            })
        }

        this.formats = formats
        selectedIndex = 0
    }

    private fun getEntryViewAt(index: Int) = getTypedViewAt<ExportFormatEntryView>(index)

    companion object {
        internal fun getSelectableItemBackground(context: Context): Drawable? {
            val theme = context.theme
            val typedValue = TypedValue()

            val isResolved =
                theme.resolveAttribute(com.google.android.material.R.attr.selectableItemBackground, typedValue, true)

            return if (isResolved) {
                ResourcesCompat.getDrawable(context.resources, typedValue.resourceId, theme)
            } else {
                null
            }
        }
    }
}