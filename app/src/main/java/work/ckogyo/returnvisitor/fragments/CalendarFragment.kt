package work.ckogyo.returnvisitor.fragments

import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.calendar_fragment.*
import kotlinx.android.synthetic.main.day_cell.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.CalendarData
import work.ckogyo.returnvisitor.models.DailyReport
import work.ckogyo.returnvisitor.utils.fadeVisibility
import work.ckogyo.returnvisitor.utils.setOnClick
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment(val month: Calendar) :Fragment() {

    private var weekStart = CalendarData.WeekStart.Monday

    private val calendarData = CalendarData(month, weekStart)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.calendar_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initDayHeaderRow()

        val handler = Handler()

        loadingCalendarOverlay.fadeVisibility(true, addTouchBlockerOnFadeIn = true)

        GlobalScope.launch {

            calendarData.prepareDataAsync().await()

            handler.post {
                for (weekReport in calendarData.weekReports) {
                    val weekRow = WeekRow(weekReport)
                    calendarFrame?.addView(weekRow)
                }
                loadingCalendarOverlay?.fadeVisibility(false)
            }

        }
    }

    private fun initDayHeaderRow() {

        val date = Calendar.getInstance()
        date.set(Calendar.DAY_OF_WEEK, calendarData.firstDayOfWeek)

        for (i in 0 until 7) {
            val cell = DayHeaderCell(date)
            dayHeaderRow.addView(cell)
            date.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private inner class DayHeaderCell(day: Calendar): TextView(context) {

        init {
            setBackgroundResource(R.drawable.gray_right_border)
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            text = SimpleDateFormat("EEE", Locale.getDefault()).format(day.time)
            setTextColor(ResourcesCompat.getColor(resources, R.color.darkGray, null))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT).also {
                it.weight = 1f
            }
        }
    }

    inner class WeekRow(weekReport: MutableList<DailyReport>): LinearLayout(context) {

        init {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0).also {
                it.weight = 1f
            }
            setBackgroundResource(R.drawable.gray_bottom_border)

            for (report in weekReport) {
                val dayCell = DayCell(report)
                addView(dayCell)
            }
        }
    }

    inner class DayCell(private val dailyReport: DailyReport): FrameLayout(context!!) {

        var onClick: ((DailyReport) -> Unit)? = null

        init {
            View.inflate(context, R.layout.day_cell, this).also {
                layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT).also {
                    it.weight = 1f
                }
            }

            dayNumberText.visibility = if (dailyReport.isDummy) {
                View.GONE
            } else {
                dayNumberText.text = dailyReport.date.get(Calendar.DAY_OF_MONTH).toString()
                View.VISIBLE
            }

            timeInDayText.visibility = if(dailyReport.isDummy) {
                View.GONE
            } else {
                timeInDayText.text = dailyReport.durationString
                View.VISIBLE
            }

            workMark.visibility = if (dailyReport.hasWork) View.VISIBLE else View.GONE
            visitMark.visibility = if (dailyReport.hasVisit) View.VISIBLE else View.GONE
            rvMark.visibility = if (dailyReport.hasRV) View.VISIBLE else View.GONE
            studyMark.visibility = if (dailyReport.hasStudy) View.VISIBLE else View.GONE
            plcMark.visibility = if (dailyReport.hasPlacement) View.VISIBLE else View.GONE
            videoMark.visibility = if (dailyReport.hasShowVideo) View.VISIBLE else View.GONE

            if (!dailyReport.isDummy && dailyReport.hasData) {
                setOnClick {
                    val fm = (context as MainActivity).supportFragmentManager
                    fm.popBackStack()

                    (context as MainActivity).showWorkFragment(dailyReport.date)
                }
            }
        }

        override fun setEnabled(enabled: Boolean) {
            super.setEnabled(enabled)

            alpha = if (enabled) 1f else 0.5f

            if (enabled) {
                setOnClick {
                    onClick?.invoke(dailyReport)
                }
            } else {
                setOnClick(null)
            }
        }
    }
}