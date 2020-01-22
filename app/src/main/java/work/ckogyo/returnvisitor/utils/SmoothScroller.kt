package work.ckogyo.returnvisitor.utils

import android.content.Context
import android.view.animation.AccelerateInterpolator
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

class SmoothScroller(recyclerView: RecyclerView): LinearSmoothScroller(recyclerView.context) {
    private var isScrolled: Boolean = false

    override fun updateActionForInterimTarget(action: Action) {
        if (isScrolled) {
            action.jumpTo(targetPosition)
        } else {
            super.updateActionForInterimTarget(action)
            action.duration *= 2
            action.interpolator = AccelerateInterpolator(1.5F)
            isScrolled = true
        }
    }
}