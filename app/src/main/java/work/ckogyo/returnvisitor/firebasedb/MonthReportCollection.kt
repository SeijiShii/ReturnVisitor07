package work.ckogyo.returnvisitor.firebasedb

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.models.DailyReport
import work.ckogyo.returnvisitor.models.MonthReport
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.utils.DataModelKeys.monthKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.yearKey
import work.ckogyo.returnvisitor.utils.FirebaseCollectionKeys.monthReportsKey
import java.util.*
import kotlin.collections.HashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MonthReportCollection {

    companion object {

        private val innerInstance = MonthReportCollection()
        val instance: MonthReportCollection
            get() = innerInstance
    }

    private fun setAsync(report: MonthReport): Deferred<Unit> {
        return GlobalScope.async {
            FirebaseDB.instance.set(monthReportsKey, report.id, report.hashMap)
        }
    }

    private suspend fun loadByMonthCore(month: Calendar): MonthReport? = suspendCoroutine { cont ->

        GlobalScope.launch {

            val userDoc = FirebaseDB.instance.userDoc
            if (userDoc == null) {
                cont.resume(null)
            } else {

                userDoc.collection(monthReportsKey)
                    .whereEqualTo(yearKey, month.get(Calendar.YEAR))
                    .whereEqualTo(monthKey, month.get(Calendar.MONTH))
                    .limit(1)
                    .get()
                    .addOnSuccessListener {
                        if (it.documents.isEmpty()) {
                            cont.resume(null)
                        } else {
                            val report = MonthReport()
                            report.initFromHashMap(it.documents[0].data as HashMap<String, Any>)
                            cont.resume(report)
                        }
                    }
                    .addOnFailureListener {
                        cont.resume(null)
                    }
            }
        }
    }

    fun updateByMonthAsync(month: Calendar): Deferred<Unit> {

        return GlobalScope.async {

            val report = loadByMonthCore(month) ?: MonthReport().also {
                it.month = month
            }

            report.calcPastCarryOverAsync().await()

            val first = month.getFirstDay()
            val last = month.getLastDay()

            val works = WorkCollection.instance.loadWorksByDateRange(first, last)
            val visits = VisitCollection.instance.loadVisitsByDateRange(first, last)

            report.calculate(works, visits)

            setAsync(report).await()
            Unit
        }
    }

    suspend fun loadByMonth(month: Calendar): MonthReport = suspendCoroutine { cont ->

        GlobalScope.launch {
            val report = loadByMonthCore(month)
            if (report != null) {
                cont.resume(report)
            } else {
                updateByMonthAsync(month).await()
                val report2 = loadByMonthCore(month)
                cont.resume(report2!!)
            }
        }
    }

//    fun prepareMonthReportsUntilNowAsync(): Deferred<Unit> {
//
//        return GlobalScope.async {
//
//            val workColl = WorkCollection.instance
//            val visitColl = VisitCollection.instance
//
//            val firstWorkMonth = workColl.getRecordedDateAtEnd(getFirst = true)
//            val firstVisitMonth = visitColl.getRecordedDateAtEnd(getFirst = true)
//
//            val firstMonth = when {
//                firstWorkMonth != null && firstVisitMonth != null -> {
//                    if (firstWorkMonth.isMonthBefore(firstVisitMonth)) {
//                        firstWorkMonth
//                    } else {
//                        firstVisitMonth
//                    }
//                }
//                firstWorkMonth != null -> firstWorkMonth
//                firstVisitMonth != null -> firstVisitMonth
//                else -> null
//            }
//
//            firstMonth ?: return@async
//
//            val monthCounter = firstMonth.clone() as Calendar
//
//            while (monthCounter.isMonthBefore(Calendar.getInstance(), allowSame = true)) {
//
//                val hasRecord = workColl.hasWorkInMonth(monthCounter) || visitColl.hasVisitInMonth(monthCounter)
//                if (hasRecord) {
//
//                    val report = loadByMonth(monthCounter)
//                    if (report == null) {
//
//                        DailyReportCollection.instance.prepareAllMonthAsync(monthCounter).await()
//                        val report2 =
//                    }
//
//
//                }
//            }
//        }
//
//    }


}