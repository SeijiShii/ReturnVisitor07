package work.ckogyo.returnvisitor.models

import work.ckogyo.returnvisitor.utils.*
import java.util.*
import kotlin.collections.ArrayList

class DailyReport(val date: Calendar) {
    var isDummy = false
    lateinit var works: ArrayList<Work>
    lateinit var visits: ArrayList<Visit>

    private val duration: Long
        get() = getTotalWorkDuration(works)

    val durationString: String
        get() {
            val dur = duration
            return if (dur <= 0) "" else dur.toDurationText()
        }

    val rvCount: Int
        get() = getRVCount(visits)

    val uniqueStudyCount: Int
        get() = getUniqueStudyCount(visits)

    val placementCount: Int
        get() = getPlacementCount(visits)

    val showVideoCount: Int
        get() = getShowVideoCount(visits)

}