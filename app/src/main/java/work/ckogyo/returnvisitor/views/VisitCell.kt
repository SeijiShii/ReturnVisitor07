package work.ckogyo.returnvisitor.views

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import kotlinx.android.synthetic.main.visit_cell.view.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.confirmDeleteVisit
import work.ckogyo.returnvisitor.utils.ratingToColorButtonResId
import work.ckogyo.returnvisitor.utils.setOnClick
import work.ckogyo.returnvisitor.utils.toDP

class VisitCell(context: Context) :FrameLayout(context) {

    lateinit var visit: Visit

    var onClickEditVisit: ((Visit) -> Unit)? = null
    var onDeleteVisitConfirmed: ((Visit) -> Unit)? = null

    init {
        View.inflate(context, R.layout.visit_cell, this).also {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, context.toDP(100))
        }

        visitMenuButton.setOnClick {
            showMenuPopup()
        }
    }

    fun refresh(visit: Visit) {
        this.visit = visit

        visitText.text = visit.toString(context)
        visitColorMark.setImageDrawable(ResourcesCompat.getDrawable(context.resources, ratingToColorButtonResId(visit.rating), null))
    }

    private fun showMenuPopup() {

        val popup = PopupMenu(context, visitMenuButton)
        popup.menuInflater.inflate(R.menu.visit_cell_menu, popup.menu)
        popup.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.edit_visit -> {
                    onClickEditVisit?.invoke(visit)
                }
                R.id.delete_visit -> {
                    confirmDeleteVisit(context, visit){
                        onDeleteVisitConfirmed?.invoke(visit)
                    }
                }
            }
            return@setOnMenuItemClickListener true
        }
        popup.show()
    }


}