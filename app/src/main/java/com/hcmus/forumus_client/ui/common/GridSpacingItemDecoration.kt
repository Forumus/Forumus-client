package com.hcmus.forumus_client.ui.common

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager

class GridSpacingItemDecoration(
    private val spacing: Int       // px
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
        val spanIndex = lp.spanIndex      // cột bắt đầu
        val groupIndex = layoutManager
            .spanSizeLookup
            .getSpanGroupIndex(position, spanCount) // hàng thứ mấy

        // Reset
        outRect.set(0, 0, 0, 0)

        // Item bình thường (spanSize < spanCount), dùng spanIndex làm "column"
        val column = spanIndex // 0..spanCount-1

        if(column != 0) {
            outRect.left = spacing
        }

        if (groupIndex > 0) { // từ hàng 2 trở đi
            outRect.top = spacing
        }
    }
}
