package work.ckogyo.returnvisitor.views

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.work_elm_cell.view.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.WorkElement
import work.ckogyo.returnvisitor.utils.getTimeText
import work.ckogyo.returnvisitor.utils.toDP

class WorkElmCell(context: Context) : FrameLayout(context) {

    init {
        View.inflate(context, R.layout.work_elm_cell, this)
        // Layout XMLでセットしているはずなのにここでやらないと効かない
        layoutParams = RecyclerView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    var dataElm: WorkElement? = null
    set(value) {
        field = value

        field ?: return

        onSetDateElm()
    }

    private fun onSetDateElm() {

        dataElm ?: return

        dateBorderCellFrame.visibility = View.GONE
        workCellFrame.visibility = View.GONE
        visitCellFrame.visibility = View.GONE

        (layoutParams as RecyclerView.LayoutParams).topMargin = 0
        (layoutParams as RecyclerView.LayoutParams).bottomMargin = 0

        when(dataElm!!.category) {
            WorkElement.Category.DateBorder -> refreshDateBorderCellFrame()
            WorkElement.Category.WorkStart,
                WorkElement.Category.WorkEnd -> refreshWorkCellFrame()
            WorkElement.Category.Visit -> refreshVisitCellFrame()
        }
    }

    private fun refreshDateBorderCellFrame() {
        dataElm ?: return
        dateBorderCellFrame.visibility = View.VISIBLE

        dateBorderText.text = android.text.format.DateFormat.getMediumDateFormat(context).format(dataElm!!.dateTime.time)
    }

    private fun refreshWorkCellFrame() {

        dataElm ?: return

        workCellFrame.visibility = View.VISIBLE

        when(dataElm!!.category) {
            WorkElement.Category.WorkStart -> {
                timeLabel.text = context.getText(R.string.start)
                (layoutParams as RecyclerView.LayoutParams).topMargin = context.toDP(3)
            }
            WorkElement.Category.WorkEnd -> {
                timeLabel.text = context.getText(R.string.end)
                (layoutParams as RecyclerView.LayoutParams).bottomMargin = context.toDP(3)
            }
        }

        timeText.text = getTimeText(dataElm!!.dateTime, false)
    }

    private fun refreshVisitCellFrame() {
        dataElm ?: return
        visitCellFrame.visibility = View.VISIBLE

        leftBorder.visibility = if (dataElm!!.isVisitInWork) {
            View.VISIBLE
        }  else {
            View.GONE
        }
        rightBorder.visibility = leftBorder.visibility
    }

    fun attacheVisitCell(visitCell: VisitCell) {
        visitCellWrapper.addView(visitCell)
    }

    fun detachVisitCell() {
        visitCellWrapper.removeAllViews()
    }



}