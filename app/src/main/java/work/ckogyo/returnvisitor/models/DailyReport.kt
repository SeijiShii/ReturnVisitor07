package work.ckogyo.returnvisitor.models

import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.utils.DataModelKeys.dateStringKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.dayOfMonthKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.durationKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.hasVisitKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.hasWorkKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.monthKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.placeIdKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.plcCountKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.rvCountKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.showVideoCountKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.uniqueStudyCountKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.yearKey
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class DailyReport: BaseDataModel {

    companion object {
        const val idPrefix = "daily_report"
    }

    constructor():super(idPrefix)

    lateinit var date: Calendar
        private set

    constructor(date: Calendar): this() {
        this.date = date
    }

    constructor(date: Calendar, works: ArrayList<Work>, visits: ArrayList<Visit>): this(date) {
        duration = getTotalWorkDuration(works)
        rvCount = getRVCount(visits)
        uniqueStudyCount = getUniqueStudyCount(visits)
        placementCount = getPlacementCount(visits)
        showVideoCount = getShowVideoCount(visits)
        hasWork = works.size > 0
        hasVisit = visits.size > 0
    }

    var isDummy = false
//    var works = ArrayList<Work>()
//    var visits = ArrayList<Visit>()
    var loaded = false

    var duration: Long = 0
        private set

    val durationString: String
        get() {
            val dur = duration
            return if (dur <= 0) "" else dur.toDurationText()
        }

    var rvCount: Int = 0
        private set

    var uniqueStudyCount: Int = 0
        private set

    var placementCount: Int = 0
        private set

    var showVideoCount: Int = 0
        private set

    var hasWork: Boolean = false
        private set

    var hasVisit: Boolean = false
        private set

    val hasRV: Boolean
        get() = rvCount > 0

    val hasStudy: Boolean
        get() = uniqueStudyCount > 0

    val hasPlacement: Boolean
        get() = placementCount > 0

    val hasShowVideo:Boolean
        get() = showVideoCount > 0

    val hasData: Boolean
        get() = hasWork || hasVisit || hasRV || hasStudy || hasShowVideo || hasPlacement

    override fun clone(): DailyReport {
        val cloned = DailyReport()

        super.cloneBaseProperties(cloned)

        cloned.date = date.clone() as Calendar
        cloned.isDummy = isDummy

        cloned.loaded = loaded
        cloned.duration = duration
        cloned.rvCount = rvCount
        cloned.uniqueStudyCount = uniqueStudyCount
        cloned.placementCount = placementCount
        cloned.showVideoCount = showVideoCount
        cloned.hasWork = hasWork
        cloned.hasVisit = hasData

        return cloned
    }

    override val hashMap: HashMap<String, Any>
        get() {
            val map = super.hashMap

            map[dateStringKey] = date.toDateString()
            map[durationKey] = duration
            map[rvCountKey] = rvCount
            map[uniqueStudyCountKey] = uniqueStudyCount
            map[plcCountKey] = placementCount
            map[showVideoCountKey] = showVideoCount
            map[hasWorkKey] = hasWork
            map[hasVisitKey] = hasVisit

            map[yearKey] = date.get(Calendar.YEAR)
            map[monthKey] = date.get(Calendar.MONTH)
            map[dayOfMonthKey] = date.get(Calendar.DAY_OF_MONTH)

            return map
        }

    override fun initFromHashMap(map: HashMap<String, Any>) {
        super.initFromHashMap(map)

        date = parseFromDateString(map[dateStringKey].toString())
        duration = map[durationKey].toString().toLong()
        rvCount = map[rvCountKey].toString().toInt()
        uniqueStudyCount = map[uniqueStudyCountKey].toString().toInt()
        placementCount = map[plcCountKey].toString().toInt()
        showVideoCount = map[showVideoCountKey].toString().toInt()
        hasWork = map[hasWorkKey].toString().toBoolean()
        hasVisit = map[hasVisitKey].toString().toBoolean()
    }
}