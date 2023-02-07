package io.github.pelmenstar1.digiDict.ui.home

import android.content.Context
import android.view.View

data class HomePageItemInflaterArgs(val onRecordClickListener: View.OnClickListener)

/**
 * Provides a set of methods that assist in creating a visual representation of [HomePageItem].
 */
interface HomePageItemInflater<TItem : HomePageItem, TStaticInfo> {
    /**
     * Gets the unique integer id of the [HomePageItem] this inflater is representing.
     * It shouldn't match among the other inflaters and **shouldn't be 0**.
     */
    val uniqueId: Int

    /**
     * Creates instance of [TStaticInfo] using given [context].
     */
    fun createStaticInfo(context: Context): TStaticInfo

    /**
     * Creates a view that will be used to visually represent the page item. The view should be designed in the way
     * that the page item this view represents can be changed via [bind].
     */
    fun createView(context: Context, staticInfo: TStaticInfo): View

    /**
     * Binds the specific [item] to the [view] created by [createView] of exactly this inflater.
     */
    fun bind(view: View, item: TItem, args: HomePageItemInflaterArgs, staticInfo: TStaticInfo)
}