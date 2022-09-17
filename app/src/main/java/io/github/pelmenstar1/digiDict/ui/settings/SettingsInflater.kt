package io.github.pelmenstar1.digiDict.ui.settings

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.createNumberRangeList
import io.github.pelmenstar1.digiDict.common.forEachWithNoIterator
import io.github.pelmenstar1.digiDict.common.getSelectableItemBackground
import io.github.pelmenstar1.digiDict.common.ui.setTextAppearance
import io.github.pelmenstar1.digiDict.prefs.AppPreferences
import kotlin.math.min

private typealias ItemContentInflaterHashMap = HashMap<
        Class<out SettingsDescriptor.ItemContent<Any>>,
        SettingsInflater.ItemContentInflater<Any, SettingsDescriptor.ItemContent<Any>, View>>

class SettingsInflater(private val context: Context) {
    @Suppress("UNCHECKED_CAST")
    fun inflate(descriptor: SettingsDescriptor, container: LinearLayout): SettingsController {
        val controller = SettingsController()

        val selectableItemBackground = context.getSelectableItemBackground()

        val titleViewInfo = createTitleViewInfo()
        val itemContainerViewInfo = createItemContainerViewInfo()
        val actionItemViewInfo = createActionItemViewInfo(selectableItemBackground)
        val linkItemViewInfo = createLinkItemViewInfo(itemContainerViewInfo, selectableItemBackground)

        val actionOnClickListener = View.OnClickListener {
            val item = it.tag as SettingsDescriptor.ActionItem
            controller.performAction(item.id)
        }

        val linkOnClickListener = View.OnClickListener {
            val item = it.tag as SettingsDescriptor.LinkItem
            controller.navigate(item.directions)
        }

        descriptor.groups.forEachWithNoIterator { group ->
            createTitleView(titleViewInfo).also {
                it.setText(group.titleRes)

                container.addView(it)
            }

            group.items.forEachWithNoIterator { item ->
                val itemContainer = when (item) {
                    is SettingsDescriptor.ContentItem<*> -> {
                        createContentItemContainer(controller, item, itemContainerViewInfo)
                    }
                    is SettingsDescriptor.LinkItem -> {
                        createLinkItemContainer(
                            item,
                            itemContainerViewInfo,
                            linkItemViewInfo,
                            linkOnClickListener
                        )
                    }
                    is SettingsDescriptor.ActionItem -> {
                        createActionItemContainer(
                            item,
                            itemContainerViewInfo,
                            actionItemViewInfo,
                            actionOnClickListener
                        )
                    }
                }

                container.addView(itemContainer)
            }
        }

        return controller
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

    private fun createItemContainerViewInfo(): ItemContainerViewInfo {
        val res = context.resources

        val padding = res.getDimensionPixelOffset(R.dimen.settingItemContainer_padding)
        val iconSize = res.getDimensionPixelSize(R.dimen.settingItemContainer_iconSize)
        val nameMarginStart = res.getDimensionPixelOffset(R.dimen.settingItemContainer_nameMarginStart)

        val nameLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL

            marginStart = nameMarginStart
            weight = 1f
        }

        val iconLayoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        return ItemContainerViewInfo(padding, iconSize, nameMarginStart, nameLayoutParams, iconLayoutParams)
    }

    private fun createLinkItemViewInfo(
        containerInfo: ItemContainerViewInfo,
        selectableItemBackground: Drawable?
    ): LinkItemViewInfo {
        val res = context.resources

        val iconDrawable = ResourcesCompat.getDrawable(res, R.drawable.ic_link, context.theme)!!
        val iconSize = containerInfo.iconSize
        val iconLayoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        return LinkItemViewInfo(selectableItemBackground, iconDrawable, iconLayoutParams)
    }

    private fun createActionItemViewInfo(selectableItemBackground: Drawable?): ActionItemViewInfo {
        val res = context.resources

        val nameVerticalPadding = res.getDimensionPixelOffset(R.dimen.settings_actionItemNameVerticalMargin)

        return ActionItemViewInfo(nameVerticalPadding, selectableItemBackground)
    }

    private fun createTitleView(info: TitleViewInfo): TextView {
        return MaterialTextView(context).apply {
            layoutParams = TITLE_LAYOUT_PARAMS

            setPadding(info.padding)
            setTextAppearance { BodyMedium }
            info.typeface?.also { typeface = it }
            setBackgroundColor(info.background)
        }
    }

    private fun createContentItemContainer(
        controller: SettingsController,
        item: SettingsDescriptor.ContentItem<out Any>,
        info: ItemContainerViewInfo
    ): ViewGroup {
        val content = item.content
        val inflater = content.getInflater()

        return LinearLayout(context).apply {
            layoutParams = ITEM_CONTAINER_LAYOUT_PARAMS
            orientation = LinearLayout.HORIZONTAL

            // Tag is needed for applySnapshot to determine whether the view is 'item container'
            tag = item

            info.padding.also {
                setPadding(
                    /* left = */ it,
                    /* top = */ it,
                    /* right = */ if (inflater.needsRightPadding) it else 0,
                    /* bottom = */it
                )
            }

            addView(createItemIconView(item.iconRes, info))
            addView(createItemNameView(item.nameRes, info))

            val contentView = inflater.createView(context, content, onValueChanged = { value ->
                controller.onValueChanged(item.preferenceEntry, value)
            })

            addView(contentView.also {
                it.layoutParams = ITEM_CONTENT_LAYOUT_PARAMS
            })
        }
    }

    private fun createLinkItemContainer(
        item: SettingsDescriptor.LinkItem,
        containerInfo: ItemContainerViewInfo,
        linkInfo: LinkItemViewInfo,
        onClickListener: View.OnClickListener
    ): ViewGroup {
        val res = context.resources

        return LinearLayout(context).apply {
            layoutParams = ITEM_CONTAINER_LAYOUT_PARAMS

            // Tag is needed for click listener
            tag = item

            orientation = LinearLayout.HORIZONTAL
            background = linkInfo.selectableItemBackground?.constantState?.newDrawable(res, context.theme)
            setPadding(containerInfo.padding)

            val iconRes = item.iconRes
            addItemIconViewIfResValid(iconRes, containerInfo)

            addView(MaterialTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER_VERTICAL

                    // If there's no icon, name text-view should be on the same place.
                    marginStart = containerInfo.nameMarginStart + if (iconRes >= 0) 0 else containerInfo.iconSize
                    weight = 1f
                }

                setTextAppearance { BodyLarge }
                text = res.getText(item.nameRes)
            })

            addView(AppCompatImageView(context).apply {
                layoutParams = linkInfo.iconLayoutParams
                setImageDrawable(linkInfo.iconDrawable)
            })

            setOnClickListener(onClickListener)
        }
    }

    private fun createActionItemContainer(
        item: SettingsDescriptor.ActionItem,
        containerInfo: ItemContainerViewInfo,
        actionInfo: ActionItemViewInfo,
        onClickListener: View.OnClickListener
    ): ViewGroup {
        val res = context.resources

        return LinearLayout(context).apply {
            layoutParams = ITEM_CONTAINER_LAYOUT_PARAMS

            // Tag is needed for click listener
            tag = item

            orientation = LinearLayout.HORIZONTAL
            background = actionInfo.selectableItemBackground?.constantState?.newDrawable(res, context.theme)
            setPadding(containerInfo.padding)

            val iconRes = item.iconRes
            addItemIconViewIfResValid(iconRes, containerInfo)

            addView(MaterialTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    val verticalMargin = actionInfo.nameVerticalMargin

                    gravity = Gravity.CENTER_VERTICAL

                    // If there's no icon, name text-view should be on the same place.
                    marginStart = containerInfo.nameMarginStart + if (iconRes >= 0) 0 else containerInfo.iconSize
                    topMargin = verticalMargin
                    bottomMargin = verticalMargin

                    weight = 1f
                }

                setTextAppearance { BodyLarge }
                text = res.getText(item.nameRes)
            })

            setOnClickListener(onClickListener)
        }
    }

    private fun ViewGroup.addItemIconViewIfResValid(@DrawableRes iconRes: Int, info: ItemContainerViewInfo) {
        if (iconRes >= 0) {
            addView(createItemIconView(iconRes, info))
        }
    }

    private fun createItemIconView(@DrawableRes iconRes: Int, info: ItemContainerViewInfo): AppCompatImageView {
        return AppCompatImageView(context).apply {
            layoutParams = info.iconLayoutParams

            setImageResource(iconRes)
        }
    }

    private fun createItemNameView(@StringRes nameRes: Int, info: ItemContainerViewInfo): MaterialTextView {
        return MaterialTextView(context).apply {
            layoutParams = info.nameLayoutParams

            setTextAppearance { BodyLarge }
            setText(nameRes)
        }
    }

    private class TitleViewInfo(
        @JvmField @ColorInt val background: Int,
        @JvmField val typeface: Typeface?,
        @JvmField val padding: Int,
    )

    private class ItemContainerViewInfo(
        @JvmField val padding: Int,
        @JvmField val iconSize: Int,
        @JvmField val nameMarginStart: Int,
        @JvmField val nameLayoutParams: LinearLayout.LayoutParams,
        @JvmField val iconLayoutParams: LinearLayout.LayoutParams
    )

    private class LinkItemViewInfo(
        @JvmField val selectableItemBackground: Drawable?,
        @JvmField val iconDrawable: Drawable,
        @JvmField val iconLayoutParams: LinearLayout.LayoutParams
    )

    private class ActionItemViewInfo(
        @JvmField val nameVerticalMargin: Int,
        @JvmField val selectableItemBackground: Drawable?
    )

    interface ItemContentInflater<T : Any, TContent : SettingsDescriptor.ItemContent<T>, TView : View> {
        val needsRightPadding: Boolean

        fun createView(context: Context, content: TContent, onValueChanged: (T) -> Unit): TView
        fun setValue(view: TView, content: TContent, value: T)
    }

    private object SwitchContentInflater :
        ItemContentInflater<Boolean, SettingsDescriptor.SwitchItemContent, SwitchMaterial> {
        @Suppress("UNCHECKED_CAST")
        private val onCheckedChangedListener = CompoundButton.OnCheckedChangeListener { view, isChecked ->
            (view.tag as (Boolean) -> Unit).also {
                it(isChecked)
            }
        }

        override val needsRightPadding: Boolean
            get() = true

        override fun createView(
            context: Context,
            content: SettingsDescriptor.SwitchItemContent,
            onValueChanged: (Boolean) -> Unit
        ): SwitchMaterial {
            return SwitchMaterial(context).also {
                it.tag = onValueChanged

                it.setOnCheckedChangeListener(onCheckedChangedListener)
            }
        }

        override fun setValue(view: SwitchMaterial, content: SettingsDescriptor.SwitchItemContent, value: Boolean) {
            view.isChecked = value
        }
    }

    private object RangeSpinnerInflater :
        ItemContentInflater<Int, SettingsDescriptor.RangeSpinnerItemContent, AppCompatSpinner> {
        private class Tag(
            @JvmField val start: Int,
            @JvmField val endInclusive: Int,
            @JvmField val step: Int,
            @JvmField val onValueChanged: (Int) -> Unit
        )

        override val needsRightPadding: Boolean
            get() = false

        private val rangeSpinnerOnItemSelected = object : AdapterView.OnItemSelectedListener {
            @Suppress("UNCHECKED_CAST")
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                (parent.tag as Tag).also {
                    val value = min(it.endInclusive, it.start + position * it.step)

                    it.onValueChanged(value)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        override fun createView(
            context: Context,
            content: SettingsDescriptor.RangeSpinnerItemContent,
            onValueChanged: (Int) -> Unit
        ): AppCompatSpinner {
            val start = content.start
            val end = content.endInclusive
            val step = content.step

            return AppCompatSpinner(context).also {
                it.tag = Tag(
                    start, end, step,
                    onValueChanged = onValueChanged
                )

                it.adapter = ArrayAdapter(
                    context,
                    android.R.layout.simple_spinner_dropdown_item,
                    createNumberRangeList(start, end, step)
                )

                it.onItemSelectedListener = rangeSpinnerOnItemSelected
            }

        }

        override fun setValue(view: AppCompatSpinner, content: SettingsDescriptor.RangeSpinnerItemContent, value: Int) {
            val start = content.start
            val end = content.endInclusive
            val step = content.step

            val constrainedValue = value.coerceIn(start, end)
            val position = (constrainedValue - start) / step

            view.setSelection(position)
        }
    }

    @Suppress("UNCHECKED_CAST")
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

        private val itemInflaters = ItemContentInflaterHashMap().apply {
            put(SwitchContentInflater)
            put(RangeSpinnerInflater)
        }

        private inline fun <reified TContent : SettingsDescriptor.ItemContent<*>> ItemContentInflaterHashMap.put(
            value: ItemContentInflater<*, TContent, out View>
        ) {
            put(TContent::class.java, value as ItemContentInflater<Any, SettingsDescriptor.ItemContent<Any>, View>)
        }

        internal fun <TValue : Any, TContent : SettingsDescriptor.ItemContent<TValue>> TContent.getInflater(): ItemContentInflater<TValue, TContent, View> {
            return itemInflaters[javaClass] as ItemContentInflater<TValue, TContent, View>
        }

        fun applySnapshot(snapshot: AppPreferences.Snapshot, container: LinearLayout) {
            for (i in 0 until container.childCount) {
                val itemContainer = container.getChildAt(i)
                val tag = itemContainer.tag

                if (tag is SettingsDescriptor.ContentItem<*>) {
                    val item = tag as SettingsDescriptor.ContentItem<Any>
                    val value = snapshot[item.preferenceEntry]
                    val contentView = (itemContainer as ViewGroup).getChildAt(ITEM_CONTENT_INDEX_IN_CONTAINER)
                    val content = item.content

                    content.getInflater().setValue(contentView, content, value)
                }
            }
        }
    }
}