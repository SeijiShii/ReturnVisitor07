package work.ckogyo.returnvisitor.views

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.placement_list_cell.view.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Placement
import work.ckogyo.returnvisitor.utils.setOnClick
import work.ckogyo.returnvisitor.utils.toDP

class PlacementListCell(context: Context) : LinearLayout(context) {

    lateinit var placement: Placement
        private set

    var onSelected: ((Placement) -> Unit)? = null
    var onDeleteConfirmed: ((PlacementListCell) -> Unit)? = null

    init {

        View.inflate(context, R.layout.placement_list_cell, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, context.toDP(50))

        editPlacementButton.setOnClick {

        }

        deletePlacementButton.setOnClick {
            confirmDeletePlacement(context, placement){
                onDeleteConfirmed?.invoke(it)
            }
        }

        setOnClick {
            onSelected?.invoke(placement)
        }
    }

    fun refresh(plc: Placement) {
        this.placement = plc
        refreshPlacementText()
    }

    private fun refreshPlacementText() {
        placementText.text = placement.toShortString(context)
    }

    private fun confirmDeletePlacement(context: Context, plc: Placement, onConfirmed: (cell: PlacementListCell) -> Unit) {

        AlertDialog.Builder(context)
            .setTitle(R.string.delete_placement)
            .setMessage(context.resources.getString(R.string.delete_placement_confirm, plc.toShortString(context)))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete){ _, _ ->
                onConfirmed(this)
            }
            .show()
    }
}