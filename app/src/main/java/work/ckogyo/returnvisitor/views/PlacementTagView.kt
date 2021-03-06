package work.ckogyo.returnvisitor.views

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.placement_tag_view.view.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Placement
import work.ckogyo.returnvisitor.utils.setOnClick

class PlacementTagView(context: Context, val placement: Placement) : FrameLayout(context), TagView {

    override var onRemoved: ((TagView) -> Unit)? = null

    override fun removeFromParent() {
        (parent as? ViewGroup)?.removeView(this)
    }

    override fun addToParent(parent: ViewGroup) {
        parent.addView(this)
    }

    override val viewWidth: Int
        get() = width

    init {
        View.inflate(context, R.layout.placement_tag_view, this).also {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        }
        placementText.text = placement.toShortString(context)
        removePlcButton.setOnClick {
            onRemoved?.invoke(this)
        }
    }
}