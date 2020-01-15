package work.ckogyo.returnvisitor.models

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.firebasedb.WorkCollection
import work.ckogyo.returnvisitor.utils.areSameDates
import work.ckogyo.returnvisitor.utils.cloneDateWith0Time
import work.ckogyo.returnvisitor.utils.isDateAfter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WorkElmList {

    companion object {
        private val innerInstance = WorkElmList()
        val instance: WorkElmList
            get() = innerInstance
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

    suspend fun generateListByDate(date: Calendar): ArrayList<WorkElement> = suspendCoroutine { cont ->
        GlobalScope.launch {
            val works = WorkCollection.instance.loadAllWorksInDate(date)
            val visits = VisitCollection.instance.loadVisitsByDate(date)

            cont.resume(generateList(works, visits))
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

    private suspend fun getFirstRecordedDate(): Calendar? = suspendCoroutine { cont ->

        GlobalScope.launch {
            val firstVisitDate = VisitCollection.instance.getFirstRecordedDate()
            val firstWorkDate = WorkCollection.instance.getFirstRecordedDate()

            when {
                firstVisitDate == null && firstWorkDate == null -> cont.resume(null)
                firstVisitDate == null -> cont.resume(firstWorkDate)
                else -> cont.resume(firstVisitDate)
            }
        }
    }

    suspend fun getListOfLatestDate(): ArrayList<WorkElement>? =  suspendCoroutine { cont ->

        GlobalScope.launch {
            val firstDate = getFirstRecordedDate()
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

}