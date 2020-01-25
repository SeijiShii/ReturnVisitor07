package work.ckogyo.returnvisitor.models

import kotlinx.coroutines.*
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.firebasedb.WorkCollection
import work.ckogyo.returnvisitor.utils.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue

class WorkElmList {

    companion object {
        private val innerInstance = WorkElmList()
        val instance: WorkElmList
            get() = innerInstance

        fun getDate(elms: ArrayList<WorkElement>): Calendar? {
            return if (elms.isEmpty()) {
                null
            } else {
                elms[0].dateTime
            }
        }

        fun mergeAvoidingDup(elms1: ArrayList<WorkElement>, elms2: ArrayList<WorkElement>): ArrayList<WorkElement> {
            val merged = ArrayList<WorkElement>(elms1)

            for (elm2 in elms2) {
                var contained = false
                for (elm in merged) {
                    if (elm2.category == WorkElement.Category.Visit
                            && elm.category == WorkElement.Category.Visit
                            && elm2.visit == elm.visit
                        || elm2.category == WorkElement.Category.WorkStart
                            && elm.category == WorkElement.Category.WorkStart
                            && elm2.work == elm.work
                        || elm2.category == WorkElement.Category.WorkEnd
                            && elm.category == WorkElement.Category.WorkEnd
                            && elm2.work == elm.work
                        || elm2.category == WorkElement.Category.DateBorder
                            && elm.category == WorkElement.Category.DateBorder
                            && areSameDates(elm2.dateTime, elm.dateTime)) {
                        contained = true
                    }
                }

                if (!contained) {
                    merged.add(elm2)
                }
            }

            merged.sortBy { elm -> elm.dateTime.timeInMillis }
            return merged
        }

        fun refreshIsVisitInWork(elms: ArrayList<WorkElement>) {

            for(elm1 in elms) {
                elm1.isVisitInWork = false
                if (elm1.category == WorkElement.Category.Visit) {
                    for (elm2 in elms) {
                        if (elm2.category == WorkElement.Category.WorkStart) {

                            if (elm2.work!!.start.timeInMillis <= elm1.dateTime.timeInMillis
                                && elm1.dateTime.timeInMillis <= elm2.work!!.end.timeInMillis) {
                                elm1.isVisitInWork = true
                                break
                            }
                        }
                    }
                }
            }

        }
    }

    private fun generateList(works: ArrayList<Work>, visits: ArrayList<Visit>): ArrayList<WorkElement> {

        var elms = ArrayList<WorkElement>()

        for (work in works) {
            elms.addAll(WorkElement.fromWork(work))
        }

        for (visit in visits) {
            val elm = WorkElement(WorkElement.Category.Visit)
            elm.visit = visit
            elms.add(elm)
        }

        elms = updateDateBorders(elms)

        elms.sortBy { e -> e.dateTime.timeInMillis }
        refreshIsVisitInWork(elms)

        return elms
    }

    suspend fun generateListByDate(date: Calendar): ArrayList<WorkElement> = suspendCoroutine { cont ->
        GlobalScope.launch {
            val works = WorkCollection.instance.loadAllWorksInDate(date)
            val visits = VisitCollection.instance.loadVisitsByDate(date)

            cont.resume(generateList(works, visits))
        }
    }

    suspend fun generateListByDateRange(start: Calendar, end: Calendar): ArrayList<WorkElement> = suspendCoroutine { cont ->
        GlobalScope.launch {
            val works = WorkCollection.instance.loadWorksByDateRange(start, end)
            val visits = VisitCollection.instance.loadVisitsByDateRange(start, end)

            cont.resume(generateList(works, visits))
        }
    }

    fun generateListByDateAsync(date: Calendar): Deferred<ArrayList<WorkElement>> {
        return GlobalScope.async {
            generateListByDate(date)
        }
    }

    private fun updateDateBorders(elms: ArrayList<WorkElement>): ArrayList<WorkElement> {

        val tmp = ArrayList<WorkElement>()
        for (elm in elms) {
            if (elm.category != WorkElement.Category.DateBorder) {
                tmp.add(elm)
            }
        }

        tmp.sortBy { elm -> elm.dateTime.timeInMillis }

        if (tmp.size > 0) {
            val borderElms = ArrayList<WorkElement>()
            val borderEml1 = WorkElement(WorkElement.Category.DateBorder)
            borderEml1.dateTime = (tmp[0].dateTime).cloneWith0Time()
            borderElms.add(borderEml1)

            if (tmp.size > 1) {
                for (i in 0 until tmp.size - 1) {
                    if (!areSameDates(tmp[i].dateTime, tmp[i + 1].dateTime)) {

                        val borderElm2 = WorkElement(WorkElement.Category.DateBorder)
                        borderElm2.dateTime = (tmp[i + 1].dateTime).cloneWith0Time()
                        borderElms.add(borderElm2)
                    }
                }
            }

            tmp.addAll(borderElms)
            tmp.sortBy { elm -> elm.dateTime.timeInMillis }
        }

        return tmp
    }


    suspend fun getNearestDateWithData(date: Calendar): Calendar? = suspendCoroutine { cont ->

        GlobalScope.launch {

            if (aDayHasElmAsync(date).await()) {
                cont.resume(date.cloneWith0Time())
                return@launch
            }

            val previousDate = getNeighboringDate(date, true)
            val nextDate = getNeighboringDate(date, false)

            val dateToReturn = when {
                previousDate != null && nextDate != null -> {
                    val prevDiff = date.getDaysDiff(previousDate).absoluteValue
                    val nextDiff = date.getDaysDiff(nextDate).absoluteValue

                    if (prevDiff < nextDiff) previousDate else nextDate
                }
                previousDate != null -> previousDate
                nextDate != null -> nextDate
                else -> null
            }

            cont.resume(dateToReturn)
            return@launch

        }
    }

    suspend fun getNeighboringDate(date: Calendar, previous: Boolean): Calendar? = suspendCoroutine { cont ->

//        val start = System.currentTimeMillis()

        GlobalScope.launch {

            val visitDateTask
                    = GlobalScope.async { VisitCollection.instance.getNeighboringDateWithData(date, previous) }
            val workDateTask
                    = GlobalScope.async { WorkCollection.instance.getNeighboringDateWithData(date, previous) }

            val visitDate  = visitDateTask.await()
            val workDate = workDateTask.await()

            val nextDate = when {
                visitDate != null && workDate != null -> {
                    if (previous) {
                        if (visitDate.after(workDate)) visitDate else workDate
                    } else {
                        if (visitDate.before(workDate)) visitDate else workDate
                    }
                }
                visitDate != null -> visitDate
                workDate != null -> workDate
                else -> null
            }

            cont.resume(nextDate)
        }
    }

    private fun hasDataInDateTimeRange(start: Calendar, end: Calendar): Boolean {

        var hasVisit = false
        var hasWork = false
        runBlocking {
            hasVisit = VisitCollection.instance.hasVisitInDateTimeRange(start, end)
            hasWork = WorkCollection.instance.hasWorkInDateTimeRange(start, end)
        }
        return hasVisit || hasWork
    }

    private fun aDayHasElmAsync(date: Calendar): Deferred<Boolean> {
        return GlobalScope.async {
            VisitCollection.instance.aDayHasVisit(date) || WorkCollection.instance.aDayHasWork(date)
        }
    }

}