package work.ckogyo.returnvisitor.views

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.TimePicker
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.work_elm_cell.view.*
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Work
import work.ckogyo.returnvisitor.models.WorkElement
import work.ckogyo.returnvisitor.services.TimeCountIntentService
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

    /**
     * WorkCellにおいて削除ボタンが押されたときに呼ばれる。これが発火した時点ではFirebaseDBに対する操作はされていない。
     */
    var onDeleteWorkClicked: ((Work) -> Unit)? = null
    var onWorkTimeChanged: ((work: Work, category: WorkElement.Category, oldTime: Calendar, newTime: Calendar) -> Unit)? = null

//    override val collapseHeight: Int
//        get() = 0
//    override val extractHeight: Int
//        get() {
//            dataElm ?: return context.toDP(60)
//
//            return when(dataElm!!.category) {
//                WorkElement.Category.DateBorder -> context.toDP(30)
//                else -> context.toDP(60)
//            }
//        }
//    override val cellId: String
//        get() {
//            dataElm ?: return ""
//
//            return when(dataElm!!.category) {
//                WorkElement.Category.DateBorder -> "date_border_${dataElm!!.dateTime.timeInMillis}"
//                WorkElement.Category.WorkStart -> "${dataElm!!.work!!.id}_start_cell"
//                WorkElement.Category.WorkEnd -> "${dataElm!!.work!!.id}_end_cell"
//                else -> "${dataElm!!.visit!!.id}_cell"
//            }
//        }

    private fun onSetDateElm() {

        dataElm ?: return

        dateBorderCellFrame.visibility = View.GONE
        workCellFrame.visibility = View.GONE
        visitCellFrame.visibility = View.GONE
        durationText.visibility = View.GONE
        workMenuButton.visibility = View.GONE
        stopTimeCountingButton.visibility = View.GONE

        workMenuButton.setOnClick(null)
        timeText.setOnClick(null)
        stopTimeCountingButton.setOnClickListener(null)

        (layoutParams as RecyclerView.LayoutParams).topMargin = 0
        (layoutParams as RecyclerView.LayoutParams).bottomMargin = 0

        when(dataElm!!.category) {
            WorkElement.Category.DateBorder -> refreshDateBorderCellFrame()
            WorkElement.Category.WorkStart,
                WorkElement.Category.WorkEnd -> refreshWorkCellFrame()
            WorkElement.Category.Visit -> refreshVisitCellFrame()
        }
    }

    fun refresh() {
        onSetDateElm()
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

                updateDurationText()

                if (TimeCountIntentService.isWorkTimeCounting(dataElm!!.work!!)) {
                    initBroadcastingReceiver()
                    workMenuButton.visibility = View.GONE
                    workMenuButton.setOnClick(null)
                } else {
                    workMenuButton.visibility = View.VISIBLE
                    workMenuButton.setOnClick {
                        showMenuPopup()
                    }
                }

                (layoutParams as RecyclerView.LayoutParams).topMargin = context.toDP(3)
            }
            WorkElement.Category.WorkEnd -> {
                timeLabel.text = context.getText(R.string.end)
                (layoutParams as RecyclerView.LayoutParams).bottomMargin = context.toDP(3)

                if (TimeCountIntentService.isWorkTimeCounting(dataElm!!.work!!)) {
                    // Workが計時中の場合
                    timeText.isEnabled = false
                    stopTimeCountingButton.visibility = View.VISIBLE
                    stopTimeCountingButton.setOnClick {
                        stopTimeCounting()
                    }
                    startStopCountingButtonBlink()
                    initBroadcastingReceiver()
                }
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

                dataElm ?: return@setPositiveButton
                dataElm!!.work ?: return@setPositiveButton

                onDeleteWorkClicked?.invoke(dataElm!!.work!!)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private var oldTime: Calendar? = null
    private fun showTimePicker() {
        context?:return

        if (dataElm!!.category != WorkElement.Category.WorkStart && dataElm!!.category != WorkElement.Category.WorkEnd) {
            throw Exception("showTimePicker in WorkElmCell called for non-work dataElm!")
        }
        dataElm!!.work ?: throw Exception("showTimePicker in WorkElmCell called for null dataElm.work!")

        val work = dataElm!!.work!!

        val timeToShow
                = if (dataElm!!.category == WorkElement.Category.WorkStart) work.start else work.end
        oldTime = timeToShow.clone() as Calendar
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

        // WorkEndのセルで時間が変更されたときWorkStart側のdurationTextが更新される必要がある。
        onWorkTimeChanged?.invoke(work, dataElm!!.category, oldTime!!, timeToSet)
        oldTime = null
    }

    private fun updateTimeText(withSecond: Boolean = false) {
        timeText.text = dataElm!!.dateTime.toTimeText(context, withSecond)
    }

    fun updateDurationText(withSecond: Boolean = false) {
        durationText.text = resources.getString(R.string.duration_placeholder, dataElm!!.work!!.duration.toDurationText(withSecond))
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
        (visitCell.parent as? ViewGroup)?.removeView(visitCell)
        visitCellWrapper.addView(visitCell)
    }

    fun detachVisitCell() {
        visitCellWrapper.removeAllViews()
    }

    val visitCell: VisitCell?
        get() {
            if (visitCellWrapper.childCount <= 0) return null
            val cell = visitCellWrapper.getChildAt(0)
            if (cell is VisitCell) return cell
            return null
        }

    private var stopButtonBlink: Animation? = null
    private fun startStopCountingButtonBlink() {
        stopButtonBlink = AnimationUtils.loadAnimation(context, R.anim.blink)
        stopTimeCountingButton.animation = stopButtonBlink
        stopButtonBlink?.start()
    }

    private lateinit var receiver: BroadcastReceiver

    private fun initBroadcastingReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                intent ?: return

                val startInMillis = intent.getLongExtra(TimeCountIntentService.startTime, 0)
                dataElm?.work?.start?.timeInMillis = startInMillis

                val endInMillis = intent.getLongExtra(TimeCountIntentService.endTime, 0)
                dataElm?.work?.end?.timeInMillis = endInMillis

                when(intent.action) {
                    TimeCountIntentService.timeCountingToActivity -> {
                        when(dataElm!!.category) {
                            WorkElement.Category.WorkStart -> {
                                handler?.post {
                                    updateDurationText(true)
                                }
                            }
                            WorkElement.Category.WorkEnd -> {
                                handler?.post {
                                    updateTimeText(true)
                                }
                            }
                        }
                    }
                    TimeCountIntentService.stopTimeCountingToActivity -> {
                        when(dataElm!!.category) {
                            WorkElement.Category.WorkStart -> {
                                handler?.post {
                                    updateDurationText()
                                    workMenuButton.visibility = View.VISIBLE
                                    workMenuButton.setOnClick {
                                        showMenuPopup()
                                    }
                                }
                            }
                            WorkElement.Category.WorkEnd -> {
                                handler?.post {
                                    timeText.isEnabled = true
                                    updateTimeText()
                                }
                            }
                        }
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, IntentFilter(TimeCountIntentService.timeCountingToActivity))
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, IntentFilter(TimeCountIntentService.stopTimeCountingToActivity))
    }

    private fun stopTimeCounting() {
        TimeCountIntentService.stopTimeCount(context)

        stopButtonBlink?.cancel()
        stopTimeCountingButton.clearAnimation()

        stopTimeCountingButton.visibility = View.GONE
    }

}