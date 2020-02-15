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
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.firebasedb.WorkCollection
import work.ckogyo.returnvisitor.models.DailyReport
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.models.Work
import work.ckogyo.returnvisitor.utils.fadeVisibility
import work.ckogyo.returnvisitor.utils.isDateBefore
import work.ckogyo.returnvisitor.utils.isSameDate
import work.ckogyo.returnvisitor.utils.setOnClick
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CalendarFragment(val month: Calendar) :Fragment() {

    enum class WeekStart{
        Sunday,
        Monday
    }

    var weekStart = WeekStart.Monday

    private val firstDayOfWeek: Int
        get() {
            return if (weekStart == WeekStart.Sunday) Calendar.SUNDAY else Calendar.MONDAY
        }

    private val lastDayOfWeek: Int
        get() {
            return if (weekStart == WeekStart.Sunday) Calendar.SATURDAY else Calendar.SUNDAY
        }

    private val dailyReports = ArrayList<DailyReport>()

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

        dailyReports.clear()

        val first = month.clone() as Calendar
        first.set(Calendar.DAY_OF_MONTH, 1)

        val last = month.clone() as Calendar
        last.set(Calendar.DAY_OF_MONTH, 1)
        last.add(Calendar.MONTH, 1)
        last.add(Calendar.DAY_OF_MONTH, -1)

        val handler = Handler()

        loadingCalendarOverlay.fadeVisibility(true, addTouchBlockerOnFadeIn = true)

        GlobalScope.launch {
            val worksInMonth = WorkCollection.instance.loadWorksByDateRange(first, last)
            val visitsInMonth = VisitCollection.instance.loadVisitsByDateRange(first, last)

            val counter = first.clone() as Calendar

            while (counter.isDateBefore(last, true)) {

                val worksInDay = ArrayList<Work>()
                for (work in worksInMonth) {
                    if (work.start.isSameDate(counter)) {
                        worksInDay.add(work)
                    }
                }

                val visitsInDay = ArrayList<Visit>()
                for (visit in visitsInMonth) {
                    if (visit.dateTime.isSameDate(counter)) {
                        visitsInDay.add(visit)
                    }
                }

                val report = DailyReport(counter.clone() as Calendar).also {
                    it.works = worksInDay
                    it.visits = visitsInDay
                }
                dailyReports.add(report)

                counter.add(Calendar.DAY_OF_MONTH, 1)
            }

            // dailyReportsの前後に週の始まり日・終わり日にそろうようダミーデータを追加する
            val leadingDummyCounter = first.clone() as Calendar
            while (leadingDummyCounter.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
                leadingDummyCounter.add(Calendar.DAY_OF_MONTH, -1)
                val dummy = DailyReport(leadingDummyCounter).also {
                    it.isDummy = true
                }
                dailyReports.add(0, dummy)
            }

            val trailingDummyCounter = last.clone() as Calendar
            while (trailingDummyCounter.get(Calendar.DAY_OF_WEEK) != lastDayOfWeek) {
                trailingDummyCounter.add(Calendar.DAY_OF_MONTH, 1)
                val dummy = DailyReport(trailingDummyCounter).also {
                    it.isDummy = true
                }
                dailyReports.add(dummy)
            }

            handler.post {
                var remaining = ArrayList(dailyReports)
                while (remaining.size > 0) {
                    val reportsInWeek = ArrayList(remaining.subList(0, 7))
                    remaining = ArrayList(remaining.subList(7, remaining.size))
                    val weekRow = WeekRow(reportsInWeek)
                    calendarFrame.addView(weekRow)
                }
                loadingCalendarOverlay.fadeVisibility(false)
            }

        }
    }

    private fun initDayHeaderRow() {

        val date = Calendar.getInstance()
        val startDay = if (weekStart == WeekStart.Monday) Calendar.MONDAY else Calendar.SUNDAY
        date.set(Calendar.DAY_OF_WEEK, startDay)

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

    inner class WeekRow(reportsInWeek: ArrayList<DailyReport>): LinearLayout(context) {

        init {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0).also {
                it.weight = 1f
            }
            setBackgroundResource(R.drawable.gray_bottom_border)

            for (report in reportsInWeek) {
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
                dayNumberText.text = dailyReport.day.get(Calendar.DAY_OF_MONTH).toString()
                View.VISIBLE
            }

            timeInDayText.visibility = if(dailyReport.isDummy) {
                View.GONE
            } else {
                timeInDayText.text = dailyReport.durationString
                View.VISIBLE
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