package io.github.pelmenstar1.digiDict.common.ui.settings

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textview.MaterialTextView
import io.github.pelmenstar1.digiDict.common.android.getColorSurfaceVariant
import io.github.pelmenstar1.digiDict.common.android.getSelectableItemBackground
import io.github.pelmenstar1.digiDict.common.forEachWithNoIterator
import io.github.pelmenstar1.digiDict.common.preferences.AppPreferences
import io.github.pelmenstar1.digiDict.common.textAppearance.TextAppearance
import io.github.pelmenstar1.digiDict.common.ui.R
import io.github.pelmenstar1.digiDict.common.ui.setTextAppearance

class SettingsInflater<TEntries : AppPreferences.Entries>(private val context: Context) {
    @Suppress("UNCHECKED_CAST")
    fun inflate(descriptor: SettingsDescriptor<TEntries>, container: LinearLayout): SettingsController<TEntries> {
        val controller = SettingsController(descriptor, container)

        val selectableItemBackground = context.getSelectableItemBackground()
        val bodyMediumTextAppearance = TextAppearance(context) { BodyMedium }
        val bodyLargeTextAppearance = TextAppearance(context) { BodyLarge }

        val titleViewInfo = createTitleViewInfo(bodyMediumTextAppearance)
        val itemContainerViewInfo = createItemContainerViewInfo(bodyLargeTextAppearance)
        val contentItemViewInfo = createContentItemViewInfo(selectableItemBackground)
        val actionItemViewInfo = createActionItemViewInfo(selectableItemBackground)
        val linkItemViewInfo = createLinkItemViewInfo(itemContainerViewInfo, selectableItemBackground)

        val contentOnClickListener = View.OnClickListener {
            val item = it.tag as SettingsDescriptor.ContentItem<Any, TEntries>
            controller.performContentItemClickListener(item.preferenceEntry)
        }

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
                    is SettingsDescriptor.ContentItem<*, *> -> {
                        createContentItemContainer(
                            controller,
                            item as SettingsDescriptor.ContentItem<out Any, TEntries>,
                            itemContainerViewInfo,
                            contentItemViewInfo,
                            contentOnClickListener
                        )
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

    private fun getTitleBackground(context: Context): Drawable {
        val colorSurfaceVariant = context.getColorSurfaceVariant(defaultColor = Color.TRANSPARENT)

        return ColorDrawable(colorSurfaceVariant)
    }

    private fun createTitleViewInfo(textAppearance: TextAppearance): TitleViewInfo {
        val background = getTitleBackground(context)

        val typeface = ResourcesCompat.getFont(context, R.font.oswald_regular)
        val padding = context.resources.getDimensionPixelOffset(R.dimen.settings_titlePadding)

        return TitleViewInfo(background, typeface, padding, textAppearance)
    }

    private fun createItemContainerViewInfo(nameTextAppearance: TextAppearance): ItemContainerViewInfo {
        val res = context.resources

        val padding = res.getDimensionPixelOffset(R.dimen.settingItemContainer_padding)
        val iconSize = res.getDimensionPixelSize(R.dimen.settingItemContainer_iconSize)
        val iconPadding = res.getDimensionPixelOffset(R.dimen.settingItemContainer_iconPadding)

        val nameLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
            weight = 1f
        }

        return ItemContainerViewInfo(
            padding,
            iconSize,
            iconPadding,
            nameLayoutParams,
            nameTextAppearance
        )
    }

    private fun createContentItemViewInfo(
        selectableItemBackground: Drawable?
    ): ContentItemViewInfo {
        return ContentItemViewInfo(selectableItemBackground)
    }

    private fun createLinkItemViewInfo(
        containerInfo: ItemContainerViewInfo,
        selectableItemBackground: Drawable?
    ): LinkItemViewInfo {
        val iconDrawable = getDrawableWithSize(R.drawable.ic_link, containerInfo.iconSize)

        return LinkItemViewInfo(selectableItemBackground, iconDrawable)
    }

    private fun createActionItemViewInfo(selectableItemBackground: Drawable?): ActionItemViewInfo {
        val res = context.resources

        val additionalVerticalPadding =
            res.getDimensionPixelOffset(R.dimen.settings_actionItemAdditionalVerticalPadding)

        return ActionItemViewInfo(additionalVerticalPadding, selectableItemBackground)
    }

    private fun createTitleView(info: TitleViewInfo): TextView {
        return MaterialTextView(context).apply {
            layoutParams = TITLE_LAYOUT_PARAMS

            setPadding(info.padding)
            info.textAppearance.apply(this)
            info.typeface?.also { typeface = it }

            background = info.backgroundDrawable
        }
    }

    private fun <TValue : Any> createContentItemContainer(
        controller: SettingsController<TEntries>,
        item: SettingsDescriptor.ContentItem<TValue, TEntries>,
        containerInfo: ItemContainerViewInfo,
        contentInfo: ContentItemViewInfo,
        onContainerClickListener: View.OnClickListener
    ): ViewGroup {
        val content = item.content
        val inflater = content.getInflater()

        return LinearLayout(context).apply {
            layoutParams = ITEM_CONTAINER_LAYOUT_PARAMS
            orientation = LinearLayout.HORIZONTAL

            // Tag is needed for applySnapshot to determine whether the view is 'item container'
            tag = item

            setPadding(containerInfo.padding)

            if (item.clickable) {
                background = copyDrawable(context, contentInfo.selectableItemBackground)

                setOnClickListener(onContainerClickListener)
            }

            addView(createItemNameView(item.nameRes, item.iconRes, containerInfo))

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
    ): View {
        val res = context.resources

        return MaterialTextView(context).apply {
            layoutParams = ITEM_CONTAINER_LAYOUT_PARAMS

            // Tag is needed for click listener
            tag = item

            val iconRes = item.iconRes
            val iconSize = containerInfo.iconSize

            val padding = containerInfo.padding
            var paddingStart = padding
            if (iconRes == null) {
                paddingStart += iconSize
            }

            setPaddingRelative(paddingStart, padding, padding, padding)
            gravity = Gravity.CENTER_VERTICAL

            val startDrawable = iconRes?.let { getDrawableWithSize(it, iconSize) }
            val endDrawable = linkInfo.iconDrawable

            compoundDrawablePadding = containerInfo.iconPadding
            setCompoundDrawablesRelative(startDrawable, null, endDrawable, null)

            containerInfo.nameTextAppearance.apply(this)
            text = res.getText(item.nameRes)

            background = copyDrawable(context, linkInfo.selectableItemBackground)

            setOnClickListener(onClickListener)
        }
    }

    private fun createActionItemContainer(
        item: SettingsDescriptor.ActionItem,
        containerInfo: ItemContainerViewInfo,
        actionInfo: ActionItemViewInfo,
        onClickListener: View.OnClickListener
    ): View {
        val res = context.resources

        return MaterialTextView(context).apply {
            layoutParams = ITEM_CONTAINER_LAYOUT_PARAMS

            // Tag is needed for click listener
            tag = item

            val iconRes = item.iconRes
            val iconSize = containerInfo.iconSize

            val additionalVerticalPadding = actionInfo.additionalVerticalPadding
            val generalPadding = containerInfo.padding
            val totalVerticalPadding = generalPadding + additionalVerticalPadding

            var paddingStart = generalPadding

            if (iconRes == null) {
                paddingStart += iconSize
            }

            setPaddingRelative(paddingStart, totalVerticalPadding, generalPadding, totalVerticalPadding)
            gravity = Gravity.CENTER_VERTICAL

            if (iconRes != null) {
                val icon = getDrawableWithSize(iconRes, iconSize)

                compoundDrawablePadding = containerInfo.iconPadding
                setCompoundDrawablesRelative(icon, null, null, null)
            }

            background = copyDrawable(context, actionInfo.selectableItemBackground)

            containerInfo.nameTextAppearance.apply(this)
            text = res.getText(item.nameRes)

            setOnClickListener(onClickListener)
        }
    }

    private fun createItemNameView(
        @StringRes nameRes: Int,
        @DrawableRes iconRes: Int?,
        info: ItemContainerViewInfo
    ): MaterialTextView {
        return MaterialTextView(context).apply {
            layoutParams = info.nameLayoutParams
            gravity = Gravity.CENTER_VERTICAL

            val iconSize = info.iconSize
            if (iconRes != null) {
                val icon = getDrawableWithSize(iconRes, iconSize)

                compoundDrawablePadding = info.iconPadding
                setCompoundDrawablesRelative(icon, null, null, null)
            } else {
                setPaddingRelative(iconSize, 0, 0, 0)
            }

            info.nameTextAppearance.apply(this)
            setText(nameRes)
        }
    }

    private fun getDrawableWithSize(@DrawableRes res: Int, size: Int): Drawable {
        return ResourcesCompat.getDrawable(context.resources, res, context.theme)!!.also {
            it.setBounds(0, 0, size, size)
        }
    }

    private fun copyDrawable(context: Context, value: Drawable?): Drawable? {
        return value?.constantState?.newDrawable(context.resources, context.theme)
    }

    private class TitleViewInfo(
        @JvmField val backgroundDrawable: Drawable,
        @JvmField val typeface: Typeface?,
        @JvmField val padding: Int,
        @JvmField val textAppearance: TextAppearance
    )

    private class ItemContainerViewInfo(
        @JvmField val padding: Int,
        @JvmField val iconSize: Int,
        @JvmField val iconPadding: Int,
        @JvmField val nameLayoutParams: LinearLayout.LayoutParams,
        @JvmField val nameTextAppearance: TextAppearance,
    )

    private class ContentItemViewInfo(
        @JvmField val selectableItemBackground: Drawable?
    )

    private class LinkItemViewInfo(
        @JvmField val selectableItemBackground: Drawable?,
        @JvmField val iconDrawable: Drawable,
    )

    private class ActionItemViewInfo(
        @JvmField val additionalVerticalPadding: Int,
        @JvmField val selectableItemBackground: Drawable?
    )

    interface ItemContentInflater<T : Any, TContent : SettingsDescriptor.ItemContent<T>, TView : View> {
        fun createView(context: Context, content: TContent, onValueChanged: (T) -> Unit): TView
        fun setValue(
            controller: SettingsController<*>,
            entry: AppPreferences.Entry<T, *>,
            view: TView,
            content: TContent,
            value: T
        )
    }

    private object SwitchContentInflater :
        ItemContentInflater<Boolean, SettingsDescriptor.SwitchItemContent, SwitchMaterial> {

        override fun createView(
            context: Context,
            content: SettingsDescriptor.SwitchItemContent,
            onValueChanged: (Boolean) -> Unit
        ): SwitchMaterial {
            return SwitchMaterial(context).also {
                it.setOnCheckedChangeListener { _, isChecked ->
                    onValueChanged(isChecked)
                }
            }
        }

        override fun setValue(
            controller: SettingsController<*>,
            entry: AppPreferences.Entry<Boolean, *>,
            view: SwitchMaterial,
            content: SettingsDescriptor.SwitchItemContent,
            value: Boolean
        ) {
            view.isChecked = value
        }
    }

    private object FormattedTextItemInflater :
        ItemContentInflater<Any, SettingsDescriptor.TextContent<Any>, MaterialTextView> {
        override fun createView(
            context: Context,
            content: SettingsDescriptor.TextContent<Any>,
            onValueChanged: (Any) -> Unit
        ): MaterialTextView {
            return MaterialTextView(context).apply {
                setTextAppearance { BodyLarge }
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun setValue(
            controller: SettingsController<*>,
            entry: AppPreferences.Entry<Any, *>,
            view: MaterialTextView,
            content: SettingsDescriptor.TextContent<Any>,
            value: Any
        ) {
            entry as AppPreferences.Entry<Any, AppPreferences.Entries>
            controller as SettingsController<AppPreferences.Entries>

            val formatter = controller.getTextFormatter(entry)
                ?: throw IllegalStateException("No formatter specified for ${entry.valueClass} class")

            view.text = formatter.format(value)
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

        private const val ITEM_CONTENT_INDEX_IN_CONTAINER = 1

        @Suppress("UNCHECKED_CAST")
        internal fun <TValue : Any> SettingsDescriptor.ItemContent<TValue>.getInflater(): ItemContentInflater<TValue, SettingsDescriptor.ItemContent<TValue>, View> {
            val inflater = when (javaClass) {
                SettingsDescriptor.SwitchItemContent::class.java -> SwitchContentInflater
                SettingsDescriptor.TextContent::class.java -> FormattedTextItemInflater
                else -> throw IllegalStateException("Invalid type of content")
            }

            return inflater as ItemContentInflater<TValue, SettingsDescriptor.ItemContent<TValue>, View>
        }

        @Suppress("UNCHECKED_CAST")
        internal fun <TEntries : AppPreferences.Entries> applySnapshot(
            controller: SettingsController<TEntries>,
            snapshot: AppPreferences.Snapshot<TEntries>,
            container: ViewGroup
        ) {
            for (i in 0 until container.childCount) {
                val itemContainer = container.getChildAt(i)
                val tag = itemContainer.tag

                if (tag is SettingsDescriptor.ContentItem<*, *>) {
                    val item = tag as SettingsDescriptor.ContentItem<Any, TEntries>
                    val prefEntry = item.preferenceEntry
                    val value = snapshot[prefEntry]
                    val contentView = (itemContainer as ViewGroup).getChildAt(ITEM_CONTENT_INDEX_IN_CONTAINER)
                    val content = item.content

                    content.getInflater().setValue(controller, prefEntry, contentView, content, value)
                }
            }
        }
    }
}