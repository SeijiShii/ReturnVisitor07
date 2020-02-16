package work.ckogyo.returnvisitor.models

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.firebasedb.WorkCollection
import work.ckogyo.returnvisitor.utils.getFirstDay
import work.ckogyo.returnvisitor.utils.getLastDay
import work.ckogyo.returnvisitor.utils.isDateBefore
import work.ckogyo.returnvisitor.utils.isSameDate
import java.lang.Exception
import java.util.*

class CalendarData(val month: Calendar, val weekStart: WeekStart) {

    enum class WeekStart{
        Sunday,
        Monday
    }

    val firstDayOfWeek: Int
        get() {
            return if (weekStart == WeekStart.Sunday) Calendar.SUNDAY else Calendar.MONDAY
        }

    val lastDayOfWeek: Int
        get() {
            return if (weekStart == WeekStart.Sunday) Calendar.SATURDAY else Calendar.SUNDAY
        }

    private val works = ArrayList<Work>()
    private val visits = ArrayList<Visit>()

    private val dailyReports = ArrayList<DailyReport>()

    private var isDataPrepared = false

    fun prepareDataAsync(): Deferred<Unit> {

        return GlobalScope.async {

            dailyReports.clear()

            val first = month.getFirstDay()
            val last = month.getLastDay()

            val worksInMonth = WorkCollection.instance.loadWorksByDateRange(first, last)
            works.clear()
            works.addAll(worksInMonth)

            val visitsInMonth = VisitCollection.instance.loadVisitsByDateRange(first, last)
            visits.clear()
            visits.addAll(visitsInMonth)

            val counter = first.clone() as Calendar

            while (counter.isDateBefore(last, true)) {

                val worksInDay = ArrayList<Work>()
                for (work in works) {
                    if (work.start.isSameDate(counter)) {
                        worksInDay.add(work)
                    }
                }

                val visitsInDay = ArrayList<Visit>()
                for (visit in visits) {
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

            isDataPrepared = true

            Unit
        }
    }

    val weekReports: ArrayList<MutableList<DailyReport>>
        get() {

            if (!isDataPrepared) {
                throw Exception("weekReports are referred before preparing data!. Call prepareData() first!")
            }

            val tmpReports = ArrayList(dailyReports)

            // dailyReportsの前後に週の始まり日・終わり日にそろうようダミーデータを追加する
            val leadingDummyCounter = tmpReports[0].date.clone() as Calendar
            while (leadingDummyCounter.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
                leadingDummyCounter.add(Calendar.DAY_OF_MONTH, -1)
                val dummy = DailyReport(leadingDummyCounter).also {
                    it.isDummy = true
                }
                tmpReports.add(0, dummy)
            }

            val trailingDummyCounter = tmpReports[tmpReports.size - 1].date.clone() as Calendar
            while (trailingDummyCounter.get(Calendar.DAY_OF_WEEK) != lastDayOfWeek) {
                trailingDummyCounter.add(Calendar.DAY_OF_MONTH, 1)
                val dummy = DailyReport(trailingDummyCounter).also {
                    it.isDummy = true
                }
                tmpReports.add(dummy)
            }

            val weeks = ArrayList<MutableList<DailyReport>>()

            var remains = ArrayList(tmpReports)
            while (remains.size > 0) {
                val week = remains.subList(0, 7)
                remains = ArrayList(remains.subList(7, remains.size))

                weeks.add(week)
            }

            return weeks
        }
}