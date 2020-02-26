package work.ckogyo.returnvisitor.models

import work.ckogyo.returnvisitor.utils.*
import java.util.*
import kotlin.collections.ArrayList

class DailyReport(val date: Calendar) {
    var isDummy = false
    var works = ArrayList<Work>()
    var visits = ArrayList<Visit>()
    var loaded = false

    val duration: Long
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

    val hasWork: Boolean
        get() = works.size > 0

    val hasVisit: Boolean
        get() = visits.size > 0

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

}