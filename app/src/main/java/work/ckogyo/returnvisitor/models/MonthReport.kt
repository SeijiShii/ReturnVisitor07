package work.ckogyo.returnvisitor.models

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendAtomicCancellableCoroutine
import work.ckogyo.returnvisitor.firebasedb.WorkCollection
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.utils.DataModelKeys.durationKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.monthKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.pastCarryOverKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.plcCountKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.rvCountKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.showVideoCountKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.studyCountKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.yearKey
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.coroutines.suspendCoroutine

class MonthReport() :
    BaseDataModel(idPrefix) {

    companion object {
        const val idPrefix = "month_report"
    }

    var month = Calendar.getInstance()

    var duration: Long = 0
        private set
    var rvCount = 0
        private set
    var plcCount = 0
        private set
    var studyCount = 0
        private set
    var showVideoCount = 0
        private set

    val carryOver: Long
        get() = duration % hourInMillis

    var pastCarryOver: Long = 0
        private set

    constructor(month: Calendar):this() {
        this.month = month
    }


    override fun clone(): MonthReport {
        val cloned = MonthReport()
        super.cloneBaseProperties(cloned)

        cloned.month = month.clone() as Calendar

        cloned.duration = duration
        cloned.rvCount = rvCount
        cloned.plcCount = plcCount
        cloned.studyCount = studyCount
        cloned.showVideoCount = showVideoCount

        return cloned
    }



    fun calculate(works: ArrayList<Work>, visits: ArrayList<Visit>) {

        duration = getTotalWorkDuration(works) + pastCarryOver
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
            map[pastCarryOverKey] = pastCarryOver

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

        val pastCarryOverObj = map[pastCarryOverKey]
        if (pastCarryOverObj != null) {
            pastCarryOver = pastCarryOverObj.toString().toLong()
        }
    }

    fun calcPastCarryOverAsync(): Deferred<Long> {
        return GlobalScope.async {
            val totalDurationUntilLastMonth = WorkCollection.instance.loadTotalDurationUntilLastMonth(month)
            pastCarryOver = totalDurationUntilLastMonth % hourInMillis
            pastCarryOver
        }
    }
}