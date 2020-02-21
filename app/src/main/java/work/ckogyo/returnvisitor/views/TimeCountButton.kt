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

    private fun initCommon() {

        collapseHeight = context.toDP(50)

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
                TimeCountIntentService.stopTimeCount(context)
            }

            refreshUIs()
        }

        startTimeText.setOnClick {
            showTimePicker()
        }

        refreshButtonText()
        initReceiver()
    }

    private fun refreshUIs() {
        animateHeight()
        refreshButtonText()
        refreshDurationText()
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
        timeCountIntent.action = TimeCountIntentService.startCountingToService
        context.startService(timeCountIntent)
    }

    private fun initReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when(intent?.action) {
                    TimeCountIntentService.timeCountingToActivity -> {
                        val duration = intent.getLongExtra(TimeCountIntentService.duration, 0)
                        refreshDurationText(duration)

                        if (TimeCountIntentService.isTimeCounting && (!isCountingTime || !isExtracted)) {
                            isCountingTime = true
                            isExtracted = true

                            val startInMillis = intent.getLongExtra(TimeCountIntentService.startTime, 0)
                            startTime = Calendar.getInstance()
                            startTime.timeInMillis = startInMillis

                            refreshStartTimeText()

                            refreshUIs()
                        }
                    }
                    TimeCountIntentService.stopTimeCountingToActivity -> {
                        isCountingTime = false
                        isExtracted = false
                        refreshUIs()
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(context!!).registerReceiver(receiver, IntentFilter(TimeCountIntentService.timeCountingToActivity))
        LocalBroadcastManager.getInstance(context!!).registerReceiver(receiver, IntentFilter(TimeCountIntentService.stopTimeCountingToActivity))
    }

    private fun refreshDurationText(duration: Long = 0) {
        durationText.text = context!!.resources.getString(R.string.duration_placeholder,
            duration.toDurationText(true))
    }

}