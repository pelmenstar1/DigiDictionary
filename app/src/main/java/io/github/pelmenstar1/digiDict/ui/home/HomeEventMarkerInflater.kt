package io.github.pelmenstar1.digiDict.ui.home

import android.content.Context
import android.view.View

object HomeEventMarkerInflater : HomePageItemInflater<HomePageItem.EventMarker, HomeEventMarkerViewStaticInfo> {
    override val uniqueId: Int
        get() = 3

    override fun createStaticInfo(context: Context) = HomeEventMarkerViewStaticInfo(context)

    override fun createView(context: Context, staticInfo: HomeEventMarkerViewStaticInfo): View {
        return HomeEventMarkerView(context, staticInfo)
    }

    override fun bind(
        view: View,
        item: HomePageItem.EventMarker,
        args: HomePageItemInflaterArgs,
        staticInfo: HomeEventMarkerViewStaticInfo
    ) {
        view as HomeEventMarkerView

        view.setContent(item.isStarted, item.event.name)
    }
}