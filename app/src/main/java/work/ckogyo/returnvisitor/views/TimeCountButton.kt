package work.ckogyo.returnvisitor.views

import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.AttributeSet
import android.view.View
import android.widget.TimePicker
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.time_count_button.view.*
import kotlinx.coroutines.channels.BroadcastChannel
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.services.TimeCountIntentService
import work.ckogyo.returnvisitor.utils.setOnClick
import work.ckogyo.returnvisitor.utils.toDP
import work.ckogyo.returnvisitor.utils.toDurationText
import java.util.*

class TimeCountButton : HeightAnimationView, TimePickerDialog.OnTimeSetListener {

    private var isCountingTime = false

    private lateinit var startTime: Calendar
    private lateinit var receiver: BroadcastReceiver

    constructor(context: Context) : super(context){initCommon()}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs){initCommon()}
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr){initCommon()}

    override val collapseHeight: Int
        get() = context.toDP(50)
    override val extractHeight: Int
        get() = context.toDP(150)
    override val cellId: String
        get() = "time_count_button"

    private fun initCommon() {
        View.inflate(context, R.layout.time_count_button, this)

        isExtracted = isCountingTime

        countButton.setOnClick {
            isCountingTime = !isCountingTime
            isExtracted = isCountingTime

            if (isCountingTime) {
                startTime = Calendar.getInstance()
                refreshStartTimeText()
                startTimeCountService()
            } else {
                TimeCountIntentService.stopTimeCount()
            }

            animateHeight()
            refreshButtonText()
        }

        startTimeText.setOnClick {
            showTimePicker()
        }

        refreshButtonText()
        initReceiver()
    }

    private fun showTimePicker() {

        TimePickerDialog(context, this, startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE), true).show()
    }

    override fun onTimeSet(p0: TimePicker?, p1: Int, p2: Int) {

        startTime.set(Calendar.HOUR_OF_DAY, p1)
        startTime.set(Calendar.MINUTE, p2)

        refreshStartTimeText()

    }

    private fun refreshStartTimeText() {

        val format = android.text.format.DateFormat.getTimeFormat(context)
        startTimeText.text = format.format(startTime.time)
    }

    private fun refreshButtonText() {
        countButton.text = context.getString(if (isCountingTime) R.string.stop_time_count else R.string.start_time_count)
    }

    private fun startTimeCountService() {
        val timeCountIntent = Intent(context, TimeCountIntentService::class.java)
        timeCountIntent.action = TimeCountIntentService.startCountToService
        context.startService(timeCountIntent)
    }

    private fun initReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == TimeCountIntentService.timeCountingToActivity) {
                    val duration = intent.getLongExtra(TimeCountIntentService.duration, 0)
                    durationText.text = context!!.resources.getString(R.string.duration_placeholder,
                                                                        duration.toDurationText(true))
                }
            }
        }
        LocalBroadcastManager.getInstance(context!!).registerReceiver(receiver, IntentFilter(TimeCountIntentService.timeCountingToActivity))
    }

}