package work.ckogyo.returnvisitor.views

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.placement_list_cell.view.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Placement
import work.ckogyo.returnvisitor.utils.setOnClick
import work.ckogyo.returnvisitor.utils.toDP

class PlacementListCell(context: Context) : LinearLayout(context) {

    private lateinit var placement: Placement

    init {

        View.inflate(context, R.layout.placement_list_cell, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, context.toDP(50))

        editPlacementButton.setOnClick {

        }

        deletePlacementButton.setOnClick {

        }

        setOnClick {

        }
    }

    fun refresh(plc: Placement) {
        this.placement = plc
        refreshPlacementText()
    }

    private fun refreshPlacementText() {
        placementText.text = placement.toShortString(context)
    }
}