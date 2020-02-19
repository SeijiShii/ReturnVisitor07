package work.ckogyo.returnvisitor.firebasedb

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.models.MonthReport
import work.ckogyo.returnvisitor.utils.*
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

    private suspend fun loadByMonth(month: Calendar): MonthReport? = suspendCoroutine { cont ->

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

    fun updateAndLoadByMonthAsync(month: Calendar): Deferred<MonthReport> {

        return GlobalScope.async {

            val report = loadByMonth(month) ?: MonthReport().also {
                it.month = month
            }

            val first = month.getFirstDay()
            val last = month.getLastDay()

            val works = WorkCollection.instance.loadWorksByDateRange(first, last)
            val visits = VisitCollection.instance.loadVisitsByDateRange(first, last)
            val totalDurationUntilLastMonth = WorkCollection.instance.loadTotalDurationUntilLastMonth(month)

            report.calculate(works, visits, totalDurationUntilLastMonth)

            setAsync(report)

            report
        }
    }


}