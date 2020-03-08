package work.ckogyo.returnvisitor.models

import kotlinx.coroutines.*
import work.ckogyo.returnvisitor.firebasedb.FirebaseDB
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

        /**
         * 一つずつ要素を追加するのでポジションなどを取りたいときはこちら
         */
        fun addElmAvoidingDup(elms: ArrayList<WorkElement>, elm: WorkElement): ArrayList<WorkElement> {

            val elms2 = ArrayList(elms)

            var contained = false
            for (elm2 in elms2) {
                if (areSameElms(elm, elm2)) {
                    contained = true
                }
            }

            if (!contained) {
                elms2.add(elm)
                elms2.sortBy { elm2 -> elm2.dateTime.timeInMillis }
            }

            return elms2
        }

        private fun areSameElms(elm1: WorkElement, elm2: WorkElement): Boolean {
            return elm1.category == WorkElement.Category.Visit
                    && elm2.category == WorkElement.Category.Visit
                    && elm1.visit == elm2.visit
                    || elm1.category == WorkElement.Category.WorkStart
                    && elm2.category == WorkElement.Category.WorkStart
                    && elm1.work == elm2.work
                    || elm1.category == WorkElement.Category.WorkEnd
                    && elm2.category == WorkElement.Category.WorkEnd
                    && elm1.work == elm2.work
                    || elm1.category == WorkElement.Category.DateBorder
                    && elm2.category == WorkElement.Category.DateBorder
                    && elm1.dateTime.isSameDate(elm2.dateTime)
        }

        /**
         * リストをまとめてマージするので挿入位置などは取れない。
         */
        fun mergeAvoidingDup(elms1: ArrayList<WorkElement>, elms2: ArrayList<WorkElement>): ArrayList<WorkElement> {
            val merged = ArrayList<WorkElement>(elms1)

            for (elm2 in elms2) {
                var contained = false
                for (elm in merged) {
                    if (areSameElms(elm, elm2)) {
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
            val db = FirebaseDB.instance
            val works = db.loadAllWorksInDate(date)
            val visits = db.loadVisitsByDate(date)

            cont.resume(generateList(works, visits))
        }
    }

    suspend fun generateListByDateRange(start: Calendar, end: Calendar): ArrayList<WorkElement> = suspendCoroutine { cont ->
        GlobalScope.launch {
            val db = FirebaseDB.instance
            val works = db.loadWorksByDateRange(start, end)
            val visits = db.loadVisitsByDateRange(start, end)

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
                    if (!tmp[i].dateTime.isSameDate(tmp[i + 1].dateTime)) {

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
                    = GlobalScope.async { FirebaseDB.instance.getNeighboringDateWithVisitData(date, previous) }
            val workDateTask
                    = GlobalScope.async { FirebaseDB.instance.getNeighboringDateWithWorkData(date, previous) }

            val visitDate  = visitDateTask.await()
            val workDate = workDateTask.await()

//            Log.d(debugTag, "Neighboring visit date with data: ${visitDate?.toJPDateText()}, previous: $previous")
//            Log.d(debugTag, "Neighboring work date with data: ${workDate?.toJPDateText()}, previous: $previous")

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
            hasVisit = FirebaseDB.instance.hasVisitInDateTimeRange(start, end)
            hasWork = FirebaseDB.instance.hasWorkInDateTimeRange(start, end)
        }
        return hasVisit || hasWork
    }

    private fun aDayHasElmAsync(date: Calendar): Deferred<Boolean> {
        return GlobalScope.async {
            aDayHasElm(date)
        }
    }

    suspend fun aDayHasElm(date: Calendar):Boolean = suspendCoroutine {  cont ->
        GlobalScope.launch {
            cont.resume(FirebaseDB.instance.aDayHasVisit(date) || FirebaseDB.instance.aDayHasWork(date))
        }
    }

}