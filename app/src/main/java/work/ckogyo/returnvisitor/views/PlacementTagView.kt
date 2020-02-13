package work.ckogyo.returnvisitor.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.placement_tag_view.view.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Placement
import work.ckogyo.returnvisitor.utils.setOnClick

class PlacementTagView(context: Context, private val placement: Placement) : FrameLayout(context) {

    var onRemoved: ((Placement) -> Unit)? = null

    init {
        View.inflate(context, R.layout.placement_tag_view, this)
        placementText.text = placement.toShortString(context)
        removePlcButton.setOnClick {
            onRemoved?.invoke(placement)
        }
    }
}