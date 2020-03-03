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
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.calendar_fragment.*
import kotlinx.android.synthetic.main.day_cell.view.*
import kotlinx.coroutines.*
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.DailyReportCollection
import work.ckogyo.returnvisitor.models.DailyReport
import work.ckogyo.returnvisitor.utils.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CalendarFragment(val month: Calendar) :Fragment() {

    private val dailyReports = ArrayList<DailyReport>()

    var onTransitToWorkFragment: (() -> Unit)? = null

    private val dayCells = ArrayList<DayCell>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.calendar_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareEmptyReports()
        refresh()
    }

    fun refresh() {

        loadingCalendarOverlay.fadeVisibility(true, addTouchBlockerOnFadeIn = true)
        initDayHeaderRow()

        calendarFrame?.removeAllViews()
        for (weekReport in getWeekReports()) {
            val weekRow = WeekRow(weekReport)
            calendarFrame?.addView(weekRow)
        }

        startPreparingDailyReport()
        loadingCalendarOverlay?.fadeVisibility(false)
    }

    private fun initDayHeaderRow() {

        val date = Calendar.getInstance()
        date.set(Calendar.DAY_OF_WEEK, CalendarPagerFragment.firstDayOfWeek)

        dayHeaderRow.removeAllViews()

        for (i in 0 until 7) {
            val cell = DayHeaderCell(date)
            dayHeaderRow.addView(cell)
            date.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun getWeekReports(): ArrayList<MutableList<DailyReport>> {

        val tmpReports = ArrayList(dailyReports)

        // dailyReportsの前後に週の始まり日・終わり日にそろうようダミーデータを追加する
        val leadingDummyCounter = tmpReports[0].date.clone() as Calendar
        while (leadingDummyCounter.get(Calendar.DAY_OF_WEEK) != CalendarPagerFragment.firstDayOfWeek) {
            leadingDummyCounter.add(Calendar.DAY_OF_MONTH, -1)
//            Log.d(debugTag, "leadingDummyCounter: ${leadingDummyCounter.toJPDateText()}")
            val date = leadingDummyCounter.clone() as Calendar
            val dummy = DailyReport(date).also {
                it.isDummy = true
            }
            tmpReports.add(0, dummy)
        }

        val trailingDummyCounter = tmpReports[tmpReports.size - 1].date.clone() as Calendar
        while (trailingDummyCounter.get(Calendar.DAY_OF_WEEK) != CalendarPagerFragment.lastDayOfWeek) {
            trailingDummyCounter.add(Calendar.DAY_OF_MONTH, 1)
//            Log.d(debugTag, "trailingDummyCounter: ${trailingDummyCounter.toJPDateText()}")
            val date = trailingDummyCounter.clone() as Calendar
            val dummy = DailyReport(date).also {
                it.isDummy = true
            }
            tmpReports.add(dummy)
        }

        val weeks = java.util.ArrayList<MutableList<DailyReport>>()

        var remains = ArrayList(tmpReports)
        while (remains.size > 0) {
            val week = remains.subList(0, 7)
            remains = ArrayList(remains.subList(7, remains.size))

            weeks.add(week)
        }

        return weeks
    }

    private fun prepareEmptyReports() {
        dailyReports.clear()

        val first = month.getFirstDay()
        val last = month.getLastDay()

        val counter = first.clone() as Calendar
        while (counter.isDateBefore(last, true)) {
            val report = DailyReport(counter.clone() as Calendar).also {
                it.isDummy = true
                it.loaded = false
            }
            dailyReports.add(report)
            counter.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun getDayCellByDate(date: Calendar): DayCell? {
        for (cell in dayCells) {
            if (date.isSameDate(cell.dailyReport.date)) {
                return cell
            }
        }
        return null
    }

    private fun getReportByDate(date: Calendar): DailyReport? {
        for (report in dailyReports) {
            if (date.isSameDate(report.date)) {
                return report
            }
        }
        return null
    }

    private var job: Job? = null

    private fun startPreparingDailyReport() {

        if (job?.isActive == true) {
            job?.cancel()
        }

        loadingCalendarOverlay.fadeVisibility(true)
        val handler = Handler()

        val first = month.getFirstDay()
        val last = month.getLastDay()

        val counter = first.clone() as Calendar

        job = GlobalScope.launch {

            while (counter.isDateBefore(last, true)) {

//                Log.d(debugTag, "counter: ${counter.toJPDateText()}")
//                Log.d(debugTag, "${this.toString()}, isActive: $isActive")

                if (!isActive) {
                    return@launch
                }

                var report = synchronized(dailyReports) {
                    getReportByDate(counter)
                }

                if (report?.loaded == true) {
                    counter.add(Calendar.DAY_OF_MONTH, 1)
                    continue
                }

//                val start = System.currentTimeMillis()
                report = DailyReportCollection.instance.loadByDate(counter.clone() as Calendar).also {
                    it.isDummy = false
                    it.loaded = true
                }

//                Log.d(debugTag, "Loading daily report for ${report.date.toJPDateText()}, took ${System.currentTimeMillis() - start}ms.")

                synchronized(dailyReports) {
                    val oldReport = getReportByDate(counter)

                    if (oldReport != null) {
                        dailyReports.remove(oldReport)
                    }

                    dailyReports.add(report)
                    dailyReports.sortBy { r -> r.date.timeInMillis }
                }

                val cell = getDayCellByDate(counter)
                if (cell != null) {
                    handler.post {
                        cell.refresh(report)
                    }
                }
                counter.add(Calendar.DAY_OF_MONTH, 1)
            }
            handler.post {
                loadingCalendarOverlay?.fadeVisibility(false)
            }
        }
    }

    private inner class DayHeaderCell(day: Calendar): AppCompatTextView(context) {

        init {
            setBackgroundResource(R.drawable.right_border_gray)
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
            setBackgroundResource(R.drawable.bottom_border_gray)

            for (report in weekReport) {

//                Log.d(debugTag, "report.date: ${report.date.toJPDateText()}")
                val cell = getDayCellByDate(report.date)
                val dayCell = if (cell == null) {
                    val cell2 = DayCell(report)
                    dayCells.add(cell2)
                    cell2
                } else {
                    cell
                }

                (dayCell.parent as? ViewGroup)?.removeView(dayCell)
                addView(dayCell)
            }
        }
    }


    inner class DayCell(var dailyReport: DailyReport): FrameLayout(context!!) {

        var onClick: ((DailyReport) -> Unit)? = null

        init {
            View.inflate(context, R.layout.day_cell, this).also {
                layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT).also {
                    it.weight = 1f
                }
            }
            refresh()
        }

        fun refresh(report: DailyReport? = null) {

            if (report != null) {
                dailyReport = report
            }

            dayNumberText.visibility = View.GONE
            timeInDayText.visibility = View.GONE
            workMark.visibility = View.GONE
            visitMark.visibility = View.GONE
            rvMark.visibility = View.GONE
            studyMark.visibility = View.GONE
            plcMark.visibility = View.GONE
            videoMark.visibility = View.GONE

            setOnClick(null)

            if (dailyReport.isDummy) {
                setBackgroundResource(R.color.lightGray)
            } else {

                setBackgroundResource(R.drawable.right_border_gray)

                dayNumberText.text = dailyReport.date.get(Calendar.DAY_OF_MONTH).toString()
                dayNumberText.visibility = View.VISIBLE

                timeInDayText.text = dailyReport.durationString
                timeInDayText.visibility = View.VISIBLE

                if (dailyReport.hasData) {
                    setOnClick {
                        onTransitToWorkFragment?.invoke()
                        (context as MainActivity).showWorkFragment(dailyReport.date)
                    }
                }

                workMark.visibility = if (dailyReport.hasWork) View.VISIBLE else View.GONE
                visitMark.visibility = if (dailyReport.hasVisit) View.VISIBLE else View.GONE
                rvMark.visibility = if (dailyReport.hasRV) View.VISIBLE else View.GONE
                studyMark.visibility = if (dailyReport.hasStudy) View.VISIBLE else View.GONE
                plcMark.visibility = if (dailyReport.hasPlacement) View.VISIBLE else View.GONE
                videoMark.visibility = if (dailyReport.hasShowVideo) View.VISIBLE else View.GONE
            }
        }
    }
}