package work.ckogyo.returnvisitor.models

import android.util.Log
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.firebasedb.WorkCollection
import work.ckogyo.returnvisitor.utils.*
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MonthReport() :
    BaseDataModel(idPrefix) {

    companion object {
        const val idPrefix = "month_report"
    }

    private var month = Calendar.getInstance()
    private var weekStart = WeekStart.Monday

    var duration: Long = 0
    var rvCount = 0
    var plcCount = 0
    var studyCount = 0
    var showVideoCount = 0

    constructor(month: Calendar, weekStart: WeekStart):this() {
        this.month = month
        this.weekStart = weekStart
    }


    override fun clone(): MonthReport {
        val cloned = MonthReport()
        super.cloneBaseProperties(cloned)

        cloned.month = month.clone() as Calendar
        cloned.weekStart = weekStart

        cloned.duration = duration
        cloned.rvCount = rvCount
        cloned.plcCount = plcCount
        cloned.studyCount = studyCount
        cloned.showVideoCount = showVideoCount

        return cloned
    }

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

            calculate(works, visits)

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

    fun calculate(works: ArrayList<Work>, visits: ArrayList<Visit>) {

        duration = getTotalWorkDuration(works)
        rvCount = getRVCount(visits)
        plcCount = getPlacementCount(visits)
        studyCount = getUniqueStudyCount(visits)
        showVideoCount = getShowVideoCount(visits)
    }

    override val hashMap: HashMap<String, Any>
        get() {
            val map = super.hashMap
            map[yearKey] = month.get(Calendar.YEAR)
            map[monthKey] = month.get(Calendar.MONTH)

            map[durationKey] = duration
            map[rvCountKey] = rvCount
            map[plcCountKey] = plcCount
            map[studyCountKey] = studyCount
            map[showVideoCountKey] = showVideoCount

            return map
        }

    override fun initFromHashMap(map: HashMap<String, Any>) {
        super.initFromHashMap(map)

        month = Calendar.getInstance().cloneWith0Time()
        month.set(Calendar.DAY_OF_MONTH, 1)

        val year = map[yearKey].toString().toInt()
        month.set(Calendar.YEAR, year)

        val m = map[monthKey].toString().toInt()
        month.set(Calendar.MONTH, m)

        duration = map[durationKey].toString().toLong()
        rvCount = map[rvCountKey].toString().toInt()
        plcCount = map[plcCountKey].toString().toInt()
        studyCount = map[studyCountKey].toString().toInt()
        showVideoCount = map[showVideoCountKey].toString().toInt()
    }
}