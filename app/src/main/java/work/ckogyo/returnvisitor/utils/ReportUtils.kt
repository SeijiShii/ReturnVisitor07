package work.ckogyo.returnvisitor.utils

import work.ckogyo.returnvisitor.models.DailyReport
import work.ckogyo.returnvisitor.models.PersonVisit
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.models.Work

fun getUniqueStudyCount(visits: ArrayList<Visit>): Int {

    val personVisits = ArrayList<PersonVisit>()
    for (v in visits) {
        for (pv in v.personVisits) {
            var alreadyCounted = false
            for (pv2 in personVisits) {
                if (pv.person == pv2.person) {
                    alreadyCounted = true
                }
            }

            if (!alreadyCounted && pv.isStudy) {
                personVisits.add(pv)
            }
        }
    }
    return personVisits.size
}

fun getRVCount(visits: ArrayList<Visit>): Int {
    var sum = 0
    for (v in visits) {
        sum += v.rvCount
    }
    return sum
}

fun getPlacementCount(visits: ArrayList<Visit>): Int {
    var sum = 0
    for (v in visits) {
        sum += v.placementCount
    }
    return sum
}

fun getShowVideoCount(visits: ArrayList<Visit>): Int {
    var sum = 0
    for (v in visits) {
        sum += v.showVideoCount
    }
    return sum
}

fun getTotalWorkDuration(works: ArrayList<Work>): Long {
    var sum = 0L
    for (work in works) {
        sum += work.duration
    }
    return sum
}

fun getTotalWorkDurationByDailyReports(reports: ArrayList<DailyReport>): Long {
    var sum = 0L
    for (work in reports) {
        sum += work.duration
    }
    return sum
}

