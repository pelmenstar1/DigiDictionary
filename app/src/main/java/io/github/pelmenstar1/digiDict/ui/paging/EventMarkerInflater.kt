package io.github.pelmenstar1.digiDict.ui.paging

import android.content.Context
import android.view.View

object EventMarkerInflater : PageItemInflater<PageItem.EventMarker, EventMarkerViewStaticInfo> {
    override val uniqueId: Int
        get() = 3

    override fun createStaticInfo(context: Context) = EventMarkerViewStaticInfo(context)

    override fun createView(context: Context, staticInfo: EventMarkerViewStaticInfo): View {
        return EventMarkerView(context, staticInfo)
    }

    override fun bind(view: View, item: PageItem.EventMarker, staticInfo: EventMarkerViewStaticInfo) {
        view as EventMarkerView

        view.setContent(item.isStarted, item.event.name)
    }
}