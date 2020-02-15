package work.ckogyo.returnvisitor.firebasedb

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.utils.isDateBefore
import work.ckogyo.returnvisitor.utils.isMonthBefore
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun loadMonthList(): ArrayList<Calendar> = suspendCoroutine { cont ->

    GlobalScope.launch {

        val workColl = WorkCollection.instance
        val visitColl = VisitCollection.instance

        val firstWork = workColl.loadWorkAtEnd(first = true)
        val lastWork = workColl.loadWorkAtEnd(first = false)

        val firstVisit = visitColl.loadVisitAtEnd(first = true)
        val lastVisit = visitColl.loadVisitAtEnd(first = false)

        val firstDate = when {
            firstWork == null && firstVisit == null -> null
            firstWork == null -> firstVisit!!.dateTime
            firstVisit == null -> firstWork.start
            else -> if (firstWork.start.isDateBefore(firstVisit.dateTime)) firstWork.start else firstVisit.dateTime
        }

        val lastDate = when {
            lastWork == null && lastVisit == null -> null
            lastWork == null -> lastVisit!!.dateTime
            lastVisit == null -> lastWork.start
            else -> if (lastWork.start.isDateBefore(lastVisit.dateTime)) lastWork.start else lastVisit.dateTime
        }

        val months = ArrayList<Calendar>()

        when {
            firstDate == null && lastDate == null -> cont.resume(months)
            firstDate == null -> {
                val month = lastDate!!.clone() as Calendar
                month.set(Calendar.DAY_OF_MONTH, 1)
                months.add(month)
            }
            lastDate == null -> {
                val month = firstDate.clone() as Calendar
                month.set(Calendar.DAY_OF_MONTH, 1)
                months.add(month)
            }
            else -> {

                val counter = firstDate.clone() as Calendar
                counter.set(Calendar.DAY_OF_MONTH, 1)

                while (counter.isMonthBefore(lastDate, true)) {

                    val month = counter.clone() as Calendar
                    months.add(month)

                    counter.add(Calendar.MONTH, 1)
                }
            }
        }
        cont.resume(months)
    }
}