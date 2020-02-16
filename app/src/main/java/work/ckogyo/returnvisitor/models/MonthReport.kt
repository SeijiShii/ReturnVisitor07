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

    var duration: Long = 0
    var rvCount = 0
    var plcCount = 0
    var studyCount = 0
    var showVideoCount = 0

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