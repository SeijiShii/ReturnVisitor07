package work.ckogyo.returnvisitor.views

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.TimePicker
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.visit_cell.view.*
import kotlinx.android.synthetic.main.work_elm_cell.view.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Work
import work.ckogyo.returnvisitor.models.WorkElement
import work.ckogyo.returnvisitor.utils.*
import java.util.*

class WorkElmCell(context: Context) : HeightAnimationView(context), TimePickerDialog.OnTimeSetListener {

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

    var onDeleteWorkConfirmed: ((Work) -> Unit)? = null
    var onWorkTimeChange: ((Work, WorkElement.Category) -> Unit)? = null

    override val collapseHeight: Int
        get() = 0
    override val extractHeight: Int
        get() {
            dataElm ?: return context.toDP(60)

            return when(dataElm!!.category) {
                WorkElement.Category.DateBorder -> context.toDP(30)
                else -> context.toDP(60)
            }
        }
    override val cellId: String
        get() {
            dataElm ?: return ""

            return when(dataElm!!.category) {
                WorkElement.Category.DateBorder -> "date_border_${dataElm!!.dateTime.timeInMillis}"
                WorkElement.Category.WorkStart -> "${dataElm!!.work!!.id}_start_cell"
                WorkElement.Category.WorkEnd -> "${dataElm!!.work!!.id}_end_cell"
                else -> "${dataElm!!.visit!!.id}_cell"
            }
        }

    private fun onSetDateElm() {

        dataElm ?: return

        dateBorderCellFrame.visibility = View.GONE
        workCellFrame.visibility = View.GONE
        visitCellFrame.visibility = View.GONE
        durationText.visibility = View.GONE
        workMenuButton.visibility = View.GONE

        workMenuButton.setOnClick(null)
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
                workMenuButton.visibility = View.VISIBLE
                workMenuButton.setOnClick {
                    showMenuPopup()
                }

                updateDurationText()
                (layoutParams as RecyclerView.LayoutParams).topMargin = context.toDP(3)
            }
            WorkElement.Category.WorkEnd -> {
                timeLabel.text = context.getText(R.string.end)
                (layoutParams as RecyclerView.LayoutParams).bottomMargin = context.toDP(3)
            }
        }

        updateTimeText()
        timeText.setOnClick {
            showTimePicker()
        }
    }

    private fun showMenuPopup() {

        val popup = PopupMenu(context, workMenuButton)
        popup.menuInflater.inflate(R.menu.work_cell_menu, popup.menu)
        popup.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.delete_work -> {
                    confirmDeleteWork()
                }
            }
            return@setOnMenuItemClickListener true
        }
        popup.show()
    }

    private fun confirmDeleteWork() {

        AlertDialog.Builder(context)
            .setTitle(R.string.delete_work)
            .setMessage(R.string.delete_work_message)
            .setPositiveButton(R.string.delete){ _, _ ->
                onDeleteWorkConfirmed?.invoke(dataElm!!.work!!)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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

        val timeToSet = (if (dataElm!!.category == WorkElement.Category.WorkStart) work.start.clone()
                                    else work.end.clone()) as Calendar

        timeToSet.set(Calendar.HOUR_OF_DAY, hourOfDay)
        timeToSet.set(Calendar.MINUTE, minute)

        // 始まりと終わりが交差する場合、1分を残す。
        if (dataElm!!.category == WorkElement.Category.WorkStart) {
            if (timeToSet.isTimeAfter(work.end, true)) {
                timeToSet.set(Calendar.HOUR_OF_DAY, work.end.get(Calendar.HOUR_OF_DAY))
                timeToSet.set(Calendar.MINUTE, work.end.get(Calendar.MINUTE))
                timeToSet.add(Calendar.MINUTE, -1)
            }
            work.start = timeToSet

        } else {
            if (timeToSet.isTimeBefore(work.start, true)) {
                timeToSet.set(Calendar.HOUR_OF_DAY, work.start.get(Calendar.HOUR_OF_DAY))
                timeToSet.set(Calendar.MINUTE, work.start.get(Calendar.MINUTE))
                timeToSet.add(Calendar.MINUTE, 1)
            }
            work.end = timeToSet
        }

        updateTimeText()
        if (dataElm!!.category == WorkElement.Category.WorkStart) {
            updateDurationText()
        }

        // TODO: WorkEndのセルで時間が変更されたときWorkStart側のdurationTextが更新される必要がある。
        onWorkTimeChange?.invoke(work, dataElm!!.category)
    }

    private fun updateTimeText() {
        timeText.text = dataElm!!.dateTime.toTimeText(context, false)
    }

    fun updateDurationText() {
        durationText.text = resources.getString(R.string.duration_placeholder, dataElm!!.work!!.duration.toDurationText())
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