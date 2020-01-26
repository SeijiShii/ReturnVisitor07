package work.ckogyo.returnvisitor.views

import android.app.TimePickerDialog
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TimePicker
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.work_elm_cell.view.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.WorkElement
import work.ckogyo.returnvisitor.utils.*
import java.util.*

class WorkElmCell(context: Context) : FrameLayout(context), TimePickerDialog.OnTimeSetListener {

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
        durationText.visibility = View.GONE
        timeText.setOnClick(null)

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

        dateBorderText.text = dataElm!!.dateTime.toDateText(context)
    }

    private fun refreshWorkCellFrame() {

        dataElm ?: return

        workCellFrame.visibility = View.VISIBLE

        when(dataElm!!.category) {
            WorkElement.Category.WorkStart -> {
                timeLabel.text = context.getText(R.string.start)
                durationText.visibility = View.VISIBLE
                durationText.text = resources.getString(R.string.duration_placeholder, dataElm!!.work!!.duration.toDurationText())
                (layoutParams as RecyclerView.LayoutParams).topMargin = context.toDP(3)
            }
            WorkElement.Category.WorkEnd -> {
                timeLabel.text = context.getText(R.string.end)
                (layoutParams as RecyclerView.LayoutParams).bottomMargin = context.toDP(3)
            }
        }

        timeText.text = dataElm!!.dateTime.toTimeText(context, false)
        timeText.setOnClick {
            showTimePicker()
        }
    }

    private fun showTimePicker() {
        context?:return

        if (dataElm!!.category != WorkElement.Category.WorkStart && dataElm!!.category != WorkElement.Category.WorkEnd) {
            throw Exception("showTimePicker in WorkElmCell called for non-work dataElm!")
        }
        dataElm!!.work ?: throw Exception("showTimePicker in WorkElmCell called for null dataElm.work!")

        val work = dataElm!!.work!!

        val timeToShow
                = if (dataElm!!.category == WorkElement.Category.WorkStart) work.start else work.end
        TimePickerDialog(context, this, timeToShow.get(Calendar.HOUR_OF_DAY), timeToShow.get(Calendar.MINUTE), true).show()

    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {

        if (dataElm!!.category != WorkElement.Category.WorkStart && dataElm!!.category != WorkElement.Category.WorkEnd) {
            throw Exception("onTimeSet in WorkElmCell called for non-work dataElm!")
        }
        dataElm!!.work ?: throw Exception("onTimeSet in WorkElmCell called for null dataElm.work!")

        val work = dataElm!!.work!!

        val timeToSet = if (dataElm!!.category == WorkElement.Category.WorkStart) work.start else work.end

        if (dataElm!!.category == WorkElement.Category.WorkStart) {



        } else {

        }
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