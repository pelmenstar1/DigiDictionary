package io.github.pelmenstar1.digiDict.common.ui.constListDialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.widget.TextViewCompat
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.common.android.MaterialDialogFragment
import io.github.pelmenstar1.digiDict.common.ui.R

abstract class AbstractConstantListDialogFragment<TValue, TRepr, TResData> : MaterialDialogFragment() {
    var onValueSelected: ((TValue) -> Unit)? = null

    protected open val useHorizontalPaddingOnItemContainer: Boolean
        get() = true

    @get:StringRes
    protected abstract val titleRes: Int

    override fun createDialogView(layoutInflater: LayoutInflater, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val res = context.resources

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL

            val verticalPadding = res.getDimensionPixelOffset(R.dimen.constListDialog_rootVerticalPadding)
            val horizontalPadding = if (useHorizontalPaddingOnItemContainer) {
                res.getDimensionPixelOffset(R.dimen.constListDialog_rootHorizontalPadding)
            } else {
                0
            }

            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        }

        root.addView(createTitleView(context))
        root.addView(createTitleDividerView(context))
        createAndAddViewsForItems(context, root)

        return ScrollView(context).apply {
            addView(root)
        }
    }

    private fun createTitleView(context: Context): TextView {
        val res = context.resources

        return MaterialTextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = res.getDimensionPixelOffset(R.dimen.constListDialog_titleMarginStart)
            }

            text = res.getText(titleRes)
            TextViewCompat.setTextAppearance(this, R.style.TextAppearance_DigiDictionary_ConstListDialog_Title)
        }
    }

    private fun createTitleDividerView(context: Context): MaterialDivider {
        val res = context.resources

        return MaterialDivider(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                val horizontalMargin = res.getDimensionPixelOffset(R.dimen.constListDialog_titleDividerMarginHorizontal)

                topMargin = res.getDimensionPixelOffset(R.dimen.constListDialog_titleDividerMarginTop)
                leftMargin = horizontalMargin
                rightMargin = horizontalMargin
            }
        }
    }

    private fun createAndAddViewsForItems(context: Context, root: LinearLayout) {
        val reprItems = getRepresentationItems()
        val values = getValues()

        val itemOnClickListener = View.OnClickListener {
            @Suppress("UNCHECKED_CAST")
            val value = it.tag as TValue

            onValueSelected?.invoke(value)
            dismiss()
        }

        val itemLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val resData = createResourcesData(context)

        for ((index, item) in reprItems.withIndex()) {
            root.addView(createViewForItem(context, item, resData).also {
                it.layoutParams = itemLayoutParams
                it.tag = values[index]

                it.setOnClickListener(itemOnClickListener)
            })
        }
    }

    protected abstract fun getRepresentationItems(): Array<out TRepr>
    protected abstract fun getValues(): Array<out TValue>

    protected abstract fun createResourcesData(context: Context): TResData
    protected abstract fun createViewForItem(context: Context, item: TRepr, resData: TResData): View
}