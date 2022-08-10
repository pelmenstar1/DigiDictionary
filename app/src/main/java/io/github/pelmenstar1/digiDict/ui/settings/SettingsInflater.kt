package io.github.pelmenstar1.digiDict.ui.settings

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.core.widget.TextViewCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.forEachWithNoIterator
import io.github.pelmenstar1.digiDict.prefs.AppPreferences

class SettingsInflater(private val context: Context) {
    private class TitleViewInfo(
        @JvmField @ColorInt val background: Int,
        @JvmField val typeface: Typeface?,
        @JvmField val padding: Int,
    )

    private class ItemContainerViewInfo(
        @JvmField val padding: Int,
        @JvmField val iconSize: Int,
        @JvmField val nameMarginStart: Int
    )

    private class ActionViewInfo(
        @JvmField val marginTop: Int
    )

    @Suppress("UNCHECKED_CAST")
    fun inflate(
        descriptor: SettingsDescriptor,
        onValueChanged: (AppPreferences.Entry<Any>, Any) -> Unit,
        actionArgs: SettingsDescriptor.ActionArgs,
        container: LinearLayout,
    ) {
        val titleViewInfo = createTitleViewInfo()
        val itemContainerViewInfo = createItemContainerViewInfo()
        val actionViewInfo = createActionViewInfo()

        val actionOnClickListener = View.OnClickListener {
            val perform = it.tag as ((SettingsDescriptor.ActionArgs) -> Unit)

            perform(actionArgs)
        }

        descriptor.blocks.forEachWithNoIterator { block ->
            createTitleView(titleViewInfo).also {
                it.setText(block.titleRes)

                container.addView(it)
            }

            when (block) {
                is SettingsDescriptor.ItemBlock -> {
                    block.items.forEachWithNoIterator { item ->
                        val itemInterface = object : SettingsDescriptor.ItemContentInterface<Any> {
                            override fun onValueChanged(value: Any) {
                                onValueChanged(item.preferenceEntry, value)
                            }
                        }

                        val contentView = item.content.createView(context, itemInterface)

                        createItemContainer(item.nameRes, item.iconRes, contentView, itemContainerViewInfo).also {
                            // Tag is needed for applySnapshot to determine whether the view is 'item container' and to retrieve
                            // item
                            it.tag = item

                            container.addView(it)
                        }
                    }
                }
                is SettingsDescriptor.ActionBlock -> {
                    block.actions.forEachWithNoIterator { action ->
                        createActionButton(action.nameRes, actionViewInfo).also {
                            it.tag = action.perform
                            it.setOnClickListener(actionOnClickListener)

                            container.addView(it)
                        }
                    }
                }
            }
        }
    }

    @ColorInt
    private fun getTitleBackground(context: Context): Int {
        val theme = context.theme

        val typedValue = TypedValue()
        val resolved = theme.resolveAttribute(
            com.google.android.material.R.attr.colorSurfaceVariant,
            typedValue,
            true
        )

        return if (resolved) {
            typedValue.data
        } else {
            Color.TRANSPARENT
        }
    }

    private fun createTitleViewInfo(): TitleViewInfo {
        val background = getTitleBackground(context)
        val typeface = ResourcesCompat.getFont(context, R.font.oswald_regular)
        val padding = context.resources.getDimensionPixelOffset(R.dimen.settings_titlePadding)

        return TitleViewInfo(background, typeface, padding)
    }

    private fun createTitleView(info: TitleViewInfo): TextView {
        return MaterialTextView(context).apply {
            layoutParams = TITLE_LAYOUT_PARAMS
            setPadding(info.padding)

            TextViewCompat.setTextAppearance(
                this,
                com.google.android.material.R.style.TextAppearance_Material3_BodyMedium
            )

            info.typeface?.also { typeface = it }
            setBackgroundColor(info.background)
        }
    }

    private fun createItemContainerViewInfo(): ItemContainerViewInfo {
        val res = context.resources

        val padding = res.getDimensionPixelOffset(R.dimen.settingItemContainer_padding)
        val iconSize = res.getDimensionPixelSize(R.dimen.settingItemContainer_iconSize)
        val nameMarginStart = res.getDimensionPixelOffset(R.dimen.settingItemContainer_nameMarginStart)

        return ItemContainerViewInfo(padding, iconSize, nameMarginStart)
    }

    private fun createItemContainer(
        @StringRes nameRes: Int,
        @DrawableRes iconRes: Int,
        contentView: View,
        info: ItemContainerViewInfo
    ): ViewGroup {
        return LinearLayout(context).apply {
            layoutParams = ITEM_CONTAINER_LAYOUT_PARAMS
            orientation = LinearLayout.HORIZONTAL

            setPadding(info.padding)

            addView(AppCompatImageView(context).apply {
                val size = info.iconSize

                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }

                setImageResource(iconRes)
            })

            addView(MaterialTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_VERTICAL

                    marginStart = info.nameMarginStart
                    weight = 1f
                }

                TextViewCompat.setTextAppearance(
                    this,
                    com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
                )

                setText(nameRes)
            })

            addView(contentView.also {
                it.layoutParams = ITEM_CONTENT_LAYOUT_PARAMS
            })
        }
    }

    private fun createActionViewInfo(): ActionViewInfo {
        val marginTop = context.resources.getDimensionPixelOffset(R.dimen.settings_actionMarginTop)

        return ActionViewInfo(marginTop)
    }

    private fun createActionButton(@StringRes nameRes: Int, info: ActionViewInfo): MaterialButton {
        return MaterialButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).also {
                it.gravity = Gravity.CENTER_HORIZONTAL
                it.topMargin = info.marginTop
            }

            setText(nameRes)
        }
    }

    companion object {
        private val ITEM_CONTAINER_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        private val ITEM_CONTENT_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        private val TITLE_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        private const val ITEM_CONTENT_INDEX_IN_CONTAINER = 2

        @Suppress("UNCHECKED_CAST")
        fun applySnapshot(snapshot: AppPreferences.Snapshot, container: LinearLayout) {
            for (i in 0 until container.childCount) {
                val itemContainer = container.getChildAt(i)
                val tag = itemContainer.tag

                if (tag is SettingsDescriptor.Item<*>) {
                    val item = tag as SettingsDescriptor.Item<Any>
                    val value = snapshot[item.preferenceEntry]
                    val contentView = (itemContainer as ViewGroup).getChildAt(ITEM_CONTENT_INDEX_IN_CONTAINER)

                    item.content.setValue(contentView, value)
                }
            }
        }
    }
}