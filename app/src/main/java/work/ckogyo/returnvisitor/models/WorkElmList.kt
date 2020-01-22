package work.ckogyo.returnvisitor.models

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.firebasedb.WorkCollection
import work.ckogyo.returnvisitor.utils.areSameDates
import work.ckogyo.returnvisitor.utils.cloneDateWith0Time
import work.ckogyo.returnvisitor.utils.isDateAfter
import work.ckogyo.returnvisitor.utils.isDateBefore
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
            borderEml1.dateTime = cloneDateWith0Time(tmp[0].dateTime)
            borderElms.add(borderEml1)

            if (tmp.size > 1) {
                for (i in 0 until tmp.size - 1) {
                    if (!areSameDates(tmp[i].dateTime, tmp[i + 1].dateTime)) {

                        val borderElm2 = WorkElement(WorkElement.Category.DateBorder)
                        borderElm2.dateTime = cloneDateWith0Time(tmp[i + 1].dateTime)
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

    fun loadListOfNeighboringDateAsync (date: Calendar, previous: Boolean): Deferred<ArrayList<WorkElement>?> {
        return GlobalScope.async {
            val neighboringDate = getNeighboringDateAsync(date, previous).await()
            neighboringDate ?: return@async null
            generateListByDate(neighboringDate)
        }
    }

//    private suspend fun loadListOfNeighboringDate(date: Calendar, previous: Boolean): ArrayList<WorkElement>? = suspendCoroutine {  cont ->
//        GlobalScope.launch {
//            val limitDate = getRecordedDateAtEnd(previous)
//            if (limitDate == null) {
//                cont.resume(null)
//            } else {
//                val increase = if (previous) -1 else 1
//                val checker = { date: Calendar ->
//                    if (previous) {
//                        isDateAfter(date, limitDate, true)
//                    } else {
//                        isDateBefore(date, limitDate, true)
//                    }
//                }
//
//                val dateCounter = date.clone() as Calendar
//                dateCounter.add(Calendar.DAY_OF_MONTH, increase)
//
//                val visitColl = VisitCollection.instance
//                val workColl = WorkCollection.instance
//
//                while (checker(dateCounter)) {
//
//                    if (visitColl.aDayHasVisit(dateCounter) || workColl.aDayHasWork(dateCounter)) {
//                        cont.resume(generateListByDate(dateCounter))
//                        return@launch
//                    }
//                    dateCounter.add(Calendar.DAY_OF_MONTH, increase)
//                }
//                cont.resume(null)
//            }
//        }
//
//    }

    fun getNeighboringDateAsync(date: Calendar, previous: Boolean): Deferred<Calendar?> {

        return GlobalScope.async {
            val limitDate = getRecordedDateAtEnd(previous)
            if (limitDate == null) {
                null
            } else {
                val increase = if (previous) -1 else 1
                val checker = { date: Calendar ->
                    if (previous) {
                        isDateAfter(date, limitDate, true)
                    } else {
                        isDateBefore(date, limitDate, true)
                    }
                }

                val dateCounter = date.clone() as Calendar
                dateCounter.add(Calendar.DAY_OF_MONTH, increase)

                val visitColl = VisitCollection.instance
                val workColl = WorkCollection.instance

                while (checker(dateCounter)) {

                    if (visitColl.aDayHasVisit(dateCounter) || workColl.aDayHasWork(dateCounter)) {
                        return@async dateCounter
                    }
                    dateCounter.add(Calendar.DAY_OF_MONTH, increase)
                }
                null
            }
        }
    }

}