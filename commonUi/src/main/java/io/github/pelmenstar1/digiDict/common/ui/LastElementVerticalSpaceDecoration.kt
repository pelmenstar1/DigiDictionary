package io.github.pelmenstar1.digiDict.common.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class LastElementVerticalSpaceDecoration(val height: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = parent.adapter
        if (adapter != null) {
            if (parent.getChildAdapterPosition(view) == adapter.itemCount - 1) {
                outRect.bottom = height
            }
        }
    }
}