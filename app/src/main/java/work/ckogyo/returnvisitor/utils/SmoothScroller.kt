package work.ckogyo.returnvisitor.utils

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

    // https://qiita.com/nshmura/items/cbf10a2f50a87e1dbd06
    // 指定した日付が見えるだけでなく、トップまで行くようにした
    override fun getVerticalSnapPreference(): Int {
        return if (mTargetVector == null || mTargetVector.y == 0f) SNAP_TO_ANY else SNAP_TO_START
    }
}