package io.github.pelmenstar1.digiDict.common.ui.constListDialog

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.ArrayRes
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.common.android.getSelectableItemBackground
import io.github.pelmenstar1.digiDict.common.textAppearance.TextAppearance
import io.github.pelmenstar1.digiDict.common.ui.R

abstract class SimpleTextConstantListDialogFragment<TValue> :
    AbstractConstantListDialogFragment<TValue, String, SimpleTextConstantListDialogFragment.ResData>() {
    @get:ArrayRes
    abstract val stringArrayResource: Int

    final override val useHorizontalPaddingOnItemContainer: Boolean
        get() = false

    final override fun getRepresentationItems(): Array<out String> {
        return requireContext().resources.getStringArray(stringArrayResource)
    }

    final override fun createResourcesData(context: Context): ResData {
        val res = context.resources

        val textVerticalPadding = res.getDimensionPixelOffset(R.dimen.simpleTextConstListDialog_textVerticalPadding)
        val textHorizontalPadding = res.getDimensionPixelOffset(R.dimen.simpleTextConstListDialog_textHorizontalPadding)
        val textAppearance = TextAppearance(context) { BodyLarge }

        val selectableItemBackground = context.getSelectableItemBackground()

        return ResData(
            textVerticalPadding,
            textHorizontalPadding,
            textAppearance,
            selectableItemBackground
        )
    }

    final override fun createViewForItem(context: Context, item: String, resData: ResData): View {
        return MaterialTextView(context).apply {
            text = item

            resData.textAppearance.apply(this)

            val verticalPadding = resData.textVerticalPadding
            val horizontalPadding = resData.textHorizontalPadding

            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

            background = resData.selectableItemBackground?.constantState?.newDrawable(context.resources)
        }
    }

    class ResData(
        @JvmField val textVerticalPadding: Int,
        @JvmField val textHorizontalPadding: Int,
        @JvmField val textAppearance: TextAppearance,
        @JvmField val selectableItemBackground: Drawable?,
    )
}