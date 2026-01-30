package com.hcmus.forumus_client.ui.common

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager

class GridSpacingItemDecoration(
    private val spacing: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val layoutManager = parent.layoutManager as? GridLayoutManager ?: return

        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return

        val spanCount = layoutManager.spanCount
        val lp = view.layoutParams as GridLayoutManager.LayoutParams
        val spanIndex = lp.spanIndex
        val groupIndex = layoutManager
            .spanSizeLookup
            .getSpanGroupIndex(position, spanCount)

        outRect.set(0, 0, 0, 0)

        val column = spanIndex

        if(column != 0) {
            outRect.left = spacing
        }

        if (groupIndex > 0) {
            outRect.top = spacing
        }
    }
}
