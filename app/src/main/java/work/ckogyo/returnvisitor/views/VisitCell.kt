package work.ckogyo.returnvisitor.views

import android.content.Context
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import kotlinx.android.synthetic.main.visit_cell.view.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.ratingToColorButtonResId
import work.ckogyo.returnvisitor.utils.setOnClick
import work.ckogyo.returnvisitor.utils.toDP

class VisitCell(context: Context, private val visit: Visit) :HeightAnimationView(context) {

    override val collapseHeight: Int
        get() = context.toDP(50)
    override val extractHeight: Int
        get() = context.toDP(100)
    override val cellId: String
        get() = visit.id

    var onClickEditVisit: ((Visit) -> Unit)? = null
    var onDeleteVisitConfirmed: ((Visit) -> Unit)? = null


    init {
        View.inflate(context, R.layout.visit_cell, this)

        visitText.text = visit.toString(context)
        visitColorMark.setImageDrawable(ResourcesCompat.getDrawable(context.resources, ratingToColorButtonResId(visit.rating), null))

        visitMenuButton.setOnClick {
            showMenuPopup()
        }
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
                    confirmDeleteVisit()
                }
            }
            return@setOnMenuItemClickListener true
        }
        popup.show()
    }

    private fun confirmDeleteVisit() {

        AlertDialog.Builder(context)
            .setTitle(R.string.delete_visit)
            .setMessage(context.resources.getString(R.string.delete_visit_confirm, visit.toDateTimeString(context)))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete){_, _ ->
                onDeleteVisitConfirmed?.invoke(visit)
            }
            .show()

    }
}