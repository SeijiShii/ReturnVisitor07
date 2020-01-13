package work.ckogyo.returnvisitor.views

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.work_elm_cell.view.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.WorkListElm
import work.ckogyo.returnvisitor.utils.getTimeText
import work.ckogyo.returnvisitor.utils.toDP

class WorkElmCell(context: Context) : FrameLayout(context) {

    init {
        View.inflate(context, R.layout.work_elm_cell, this)
        // Layout XMLでセットしているはずなのにここでやらないと効かない
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    var dataElm: WorkListElm? = null
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

        when(dataElm!!.category) {
            WorkListElm.Category.DateBorder -> refreshDateBorderCellFrame()
            WorkListElm.Category.WorkStart,
                WorkListElm.Category.WorkEnd -> refreshWorkCellFrame()
            WorkListElm.Category.Visit -> refreshVisitCellFrame()
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
            WorkListElm.Category.WorkStart -> {
                timeLabel.text = context.getText(R.string.start)
                (layoutParams as LayoutParams).topMargin = context.toDP(5)
            }
            WorkListElm.Category.WorkEnd -> {
                timeLabel.text = context.getText(R.string.end)
            }
        }

        timeText.text = getTimeText(dataElm!!.dateTime, false)
    }

    private fun refreshVisitCellFrame() {
        dataElm ?: return
        visitCellFrame.visibility = View.VISIBLE
    }


}