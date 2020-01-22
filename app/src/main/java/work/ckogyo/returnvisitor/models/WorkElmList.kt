package work.ckogyo.returnvisitor.models

import android.util.Log
import kotlinx.coroutines.*
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.firebasedb.WorkCollection
import work.ckogyo.returnvisitor.utils.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

        return elms
    }

    private suspend fun generateListByDate(date: Calendar): ArrayList<WorkElement> = suspendCoroutine { cont ->
        GlobalScope.launch {
            val works = WorkCollection.instance.loadAllWorksInDate(date)
            val visits = VisitCollection.instance.loadVisitsByDate(date)

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

    private suspend fun getRecordedDateAtEnd(getFirst: Boolean): Calendar? = suspendCoroutine { cont ->

        GlobalScope.launch {
            val firstVisitDate = VisitCollection.instance.getRecordedDateAtEnd(getFirst)
            val firstWorkDate = WorkCollection.instance.getRecordedDateAtEnd(getFirst)

            when {
                firstVisitDate == null && firstWorkDate == null -> cont.resume(null)
                firstVisitDate == null -> cont.resume(firstWorkDate)
                else -> cont.resume(firstVisitDate)
            }
        }
    }

    suspend fun getListOfToday(): ArrayList<WorkElement>? =  suspendCoroutine { cont ->

        GlobalScope.launch {
            val firstDate = getRecordedDateAtEnd(getFirst = true)
            if (firstDate == null) {
                cont.resume(null)
            } else {
                val dateCounter = Calendar.getInstance()
                val visitColl = VisitCollection.instance
                val workColl = WorkCollection.instance

                while (isDateAfter(dateCounter, firstDate, true)) {
                    if (visitColl.aDayHasVisit(dateCounter) || workColl.aDayHasWork(dateCounter)) {
                        cont.resume(generateListByDate(dateCounter))
                        return@launch
                    }
                    dateCounter.add(Calendar.DAY_OF_MONTH, -1)
                }
                cont.resume(null)
            }
        }
    }

//    fun getNeighboringDateAsync(date: Calendar, previous: Boolean): Deferred<Calendar?> {
//
//        return GlobalScope.async {
//            getNeighboringDate(date, previous)
//        }
//    }

    private suspend fun getPreviouslyNeighboringDate(date: Calendar): Calendar? = suspendCoroutine { cont ->
        GlobalScope.launch {

            val limitDate = date.cloneWith0Time()
            limitDate.timeInMillis -= 1

            val visitColl = VisitCollection.instance
            val workColl = WorkCollection.instance

            val hasVisitBefore = visitColl.hasVisitBeforeThan(date, includesEqual = true)
            val hasWorkBefore = workColl.hasWorkBeforeThan(date, includesEqual = true)

            if (!hasVisitBefore && !hasWorkBefore) {
                cont.resume(null)
                return@launch
            }

            val firstVisitDate = visitColl.getRecordedDateAtEnd(true)
            val firstWorkDate = workColl.getRecordedDateAtEnd(true)

            var firstDate = Calendar.getInstance()
            when {
                firstVisitDate != null && firstWorkDate != null -> {
                    firstDate = if (firstVisitDate.before(firstWorkDate)) {
                        firstVisitDate
                    } else {
                        firstWorkDate
                    }
                }
                firstVisitDate != null -> {
                    firstDate = firstVisitDate
                }
                firstWorkDate != null -> {
                    firstDate = firstWorkDate
                }
                else -> {
                    cont.resume(null)
                    return@launch
                }
            }

            cont.resume(getEndDateWithData(firstDate, limitDate, firstEnd = false))
            return@launch
        }
    }

    private suspend fun getNextNeighboringDate(date: Calendar): Calendar? = suspendCoroutine { cont ->
        GlobalScope.launch {

            val limitDate = date.cloneWith0Time()
            limitDate.add(Calendar.DAY_OF_YEAR, 1)

            val visitColl = VisitCollection.instance
            val workColl = WorkCollection.instance

            val hasVisitAfter = visitColl.hasVisitAfterThan(date, includesEqual = true)
            val hasWorkAfter = workColl.hasWorkAfterThan(date, includesEqual = true)

            if (!hasVisitAfter && !hasWorkAfter) {
                cont.resume(null)
                return@launch
            }

            val lastVisitDate = visitColl.getRecordedDateAtEnd(false)
            val lastWorkDate = workColl.getRecordedDateAtEnd(false)

            var lastDate = Calendar.getInstance()
            when {
                lastVisitDate != null && lastWorkDate != null -> {
                    lastDate = if (lastVisitDate.after(lastWorkDate)) {
                        lastVisitDate
                    } else {
                        lastWorkDate
                    }
                }
                lastVisitDate != null -> {
                    lastDate = lastVisitDate
                }
                lastWorkDate != null -> {
                    lastDate = lastWorkDate
                }
                else -> {
                    cont.resume(null)
                    return@launch
                }
            }

            cont.resume(getEndDateWithData(limitDate, lastDate, firstEnd = true))
            return@launch
        }
    }

    suspend fun getNeighboringDate(date: Calendar, previous: Boolean): Calendar? = suspendCoroutine {  cont ->
        GlobalScope.launch {
            if (previous) {
                cont.resume(getPreviouslyNeighboringDate(date))
                return@launch
            } else {
                cont.resume(getNextNeighboringDate(date))
                return@launch
            }
        }
    }


    private fun getEndDateWithData(start: Calendar, end: Calendar, firstEnd: Boolean): Calendar? {

        Log.d(debugTag, "start: ${start.toDateTimeText()}, end: ${end.toDateTimeText()}")
        if (areSameDates(start, end)) {
            return if (firstEnd) start else end
        } else {
            val boundaryInMillis = (start.timeInMillis + end.timeInMillis) / 2
            val frontEnd = Calendar.getInstance()
            frontEnd.timeInMillis = boundaryInMillis

            val latterStart = Calendar.getInstance()
            latterStart.timeInMillis = boundaryInMillis + 1

            val frontHalfHasData = hasDataInDateTimeRange(start, frontEnd)
            val latterHalfHasData = hasDataInDateTimeRange(latterStart, end)

            if (firstEnd) {
                return when {
                    frontHalfHasData && latterHalfHasData -> getEndDateWithData(start, frontEnd, firstEnd)
                    frontHalfHasData -> getEndDateWithData(start, frontEnd, firstEnd)
                    latterHalfHasData -> getEndDateWithData(latterStart, end, firstEnd)
                    else -> {
                        null
                    }
                }
            } else {
                return when {
                    frontHalfHasData && latterHalfHasData -> getEndDateWithData(latterStart, end, firstEnd)
                    latterHalfHasData -> getEndDateWithData(latterStart, end, firstEnd)
                    frontHalfHasData -> getEndDateWithData(start, frontEnd, firstEnd)
                    else -> {
                        null
                    }
                }
            }
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

    fun aDayHasElmAsync(date: Calendar): Deferred<Boolean> {
        return GlobalScope.async {
            VisitCollection.instance.aDayHasVisit(date) || WorkCollection.instance.aDayHasWork(date)
        }
    }

}