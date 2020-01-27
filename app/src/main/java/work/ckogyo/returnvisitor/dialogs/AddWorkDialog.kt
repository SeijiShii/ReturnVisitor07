package work.ckogyo.returnvisitor.dialogs

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.add_work_dialog.*
import kotlinx.android.synthetic.main.add_work_dialog.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.WorkCollection
import work.ckogyo.returnvisitor.models.Work
import work.ckogyo.returnvisitor.utils.*
import java.util.*

class AddWorkDialog : DialogFragment(),
                        DatePickerDialog.OnDateSetListener,
                        TimePickerDialog.OnTimeSetListener {

    private lateinit var dialog: AlertDialog
    private lateinit var contentView: View
    private lateinit var work: Work

    var onWorkAdded: ((Work) -> Unit)? = null

    private enum class TimeCategory {
        Start,
        End,
        None
    }

    private var timeCategory = TimeCategory.None

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        work = Work()
        work.start = Calendar.getInstance()
        work.end = Calendar.getInstance()
        work.end.add(Calendar.MINUTE, 5)

        contentView = View.inflate(context, R.layout.add_work_dialog, null)
        val handler = Handler()

        dialog = AlertDialog.Builder(context)
            .setTitle(R.string.add_work)
            .setView(contentView)
            .setPositiveButton(R.string.add){_, _ ->
                GlobalScope.launch {
                    WorkCollection.instance.set(work)
                    handler.post {
                        onWorkAdded?.invoke(work)
                    }
                }
            }
            .setNegativeButton(R.string.cancel){_, _ -> }
            .create()

        updateDateText()
        contentView.dateText.setOnClick {
            showDatePicker()
        }

        updateStartTimeText()
        contentView.startTimeText.setOnClick {
            timeCategory = TimeCategory.Start
            showTimePicker()
        }

        updateEndTimeText()
        contentView.endTimeText.setOnClick {
            timeCategory = TimeCategory.End
            showTimePicker()
        }

        updateDurationText()

        return dialog
    }

    private fun showDatePicker() {
        context?:return

        DatePickerDialog(context!!,
            this,
            work.start.get(Calendar.YEAR),
            work.start.get(Calendar.MONTH),
            work.start.get(Calendar.DAY_OF_MONTH)).show()
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {

        val tmpDate = Calendar.getInstance()
        tmpDate.set(Calendar.YEAR, year)
        tmpDate.set(Calendar.MONTH, month)
        tmpDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)

        val today = Calendar.getInstance()
        if (tmpDate.isDateAfter(today)) {
            tmpDate.copyDateFrom(today)
        }

        work.start.copyDateFrom(tmpDate)
        work.end.copyDateFrom(tmpDate)

        updateDateText()
    }

    private fun showTimePicker() {
        context?:return

        val timeToSet = when(timeCategory) {
            TimeCategory.Start -> work.start
            TimeCategory.End -> work.end
            else -> null
        }

        timeToSet ?: return

        TimePickerDialog(context!!,
            this,
            timeToSet.get(Calendar.HOUR_OF_DAY),
            timeToSet.get(Calendar.MINUTE),
            true).show()
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {

        val tmpDate = Calendar.getInstance()
        tmpDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
        tmpDate.set(Calendar.MINUTE, minute)

        when (timeCategory) {
            TimeCategory.Start -> {
                if (tmpDate.isTimeAfter(work.end, true)) {
                    tmpDate.copyTimeFrom(work.end)
                    tmpDate.add(Calendar.MINUTE, -1)
                }
                work.start = tmpDate
                updateStartTimeText()
            }
            TimeCategory.End -> {
                if (tmpDate.isTimeBefore(work.start, true)) {
                    tmpDate.copyTimeFrom(work.start)
                    tmpDate.add(Calendar.MINUTE, 1)
                }
                work.end = tmpDate
                updateEndTimeText()
            }
            else -> return
        }
        updateDurationText()
        timeCategory = TimeCategory.None
    }

    private fun updateDateText() {
        contentView.dateText.text = work.start.toDateText(context!!)
    }

    private fun updateStartTimeText() {
        contentView.startTimeText.text = work.start.toTimeText(context!!)
    }

    private fun updateEndTimeText() {
        contentView.endTimeText.text = work.end.toTimeText(context!!)
    }

    private fun updateDurationText() {
        contentView.durationText.text = work.duration.toDurationText()
    }

}