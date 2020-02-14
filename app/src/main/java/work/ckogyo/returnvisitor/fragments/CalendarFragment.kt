package work.ckogyo.returnvisitor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.day_cell.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.firebasedb.WorkCollection
import work.ckogyo.returnvisitor.models.DailyReport
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.models.Work
import work.ckogyo.returnvisitor.utils.isDateBefore
import work.ckogyo.returnvisitor.utils.isSameDate
import work.ckogyo.returnvisitor.utils.setOnClick
import java.util.*
import kotlin.collections.ArrayList

class CalendarFragment(private val month: Calendar) :Fragment() {

    private val dailyReports = ArrayList<DailyReport>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return  LinearLayout(context).also {
            it.orientation = LinearLayout.HORIZONTAL
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dailyReports.clear()

        val first = month.clone() as Calendar
        first.set(Calendar.DAY_OF_MONTH, 1)

        val last = month.clone() as Calendar
        last.set(Calendar.DAY_OF_MONTH, 1)
        last.add(Calendar.MONTH, 1)
        last.add(Calendar.DAY_OF_MONTH, -1)

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

                val report = DailyReport(counter.clone() as Calendar, worksInDay, visitsInDay)
                dailyReports.add(report)

                counter.add(Calendar.DAY_OF_MONTH, 1)
            }

        }




    }

    inner class WeekRow: LinearLayout(context) {

        init {
            orientation = HORIZONTAL
        }
    }

    inner class DayCell(private val dailyReport: DailyReport): FrameLayout(context!!) {

        var onClick: ((DailyReport) -> Unit)? = null

        init {
            View.inflate(context, R.layout.day_cell, this)
            dayNumberText.text = dailyReport.day.get(Calendar.DAY_OF_MONTH).toString()
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