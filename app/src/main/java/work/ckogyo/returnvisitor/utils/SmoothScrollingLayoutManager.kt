package work.ckogyo.returnvisitor.utils

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SmoothScrollingLayoutManager(context: Context) : LinearLayoutManager(context) {

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State?,
        position: Int
    ) {
        SmoothScroller(recyclerView).let {
            it.targetPosition = position
            startSmoothScroll(it)
        }
    }

}