package work.ckogyo.returnvisitor.firebasedb

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.models.DailyReport
import work.ckogyo.returnvisitor.utils.DataModelKeys.dateStringKey
import work.ckogyo.returnvisitor.utils.FirebaseCollectionKeys.dailyReportsKey
import work.ckogyo.returnvisitor.utils.toDateString
import java.util.*
import kotlin.collections.HashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class DailyReportCollection {

    companion object {

        private val innerInstance = DailyReportCollection()
        val instance: DailyReportCollection
            get() = innerInstance
    }

    private suspend fun loadByDateCore(date: Calendar): DailyReport? = suspendCoroutine {  cont ->
        GlobalScope.launch {
            val userDoc = FirebaseDB.instance.userDoc
            if (userDoc == null) {
                cont.resume(null)
            } else {
                val dateStr = date.toDateString()

                userDoc.collection(dailyReportsKey).whereEqualTo(dateStringKey, dateStr)
                    .limit(1)
                    .get()
                    .addOnSuccessListener {
                        if (it.documents.isEmpty()) {
                            cont.resume(null)
                        } else {
                            val report = DailyReport()
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

    private fun setAsync(report: DailyReport): Deferred<Unit> {
        return GlobalScope.async {

            val report2 = loadByDateCore(report.date)

            if (report2 != null) {
                report.id = report2.id
            }
            FirebaseDB.instance.set(dailyReportsKey, report.id, report.hashMap)
        }
    }

    private fun initDailyReportAsync(date: Calendar) :Deferred<DailyReport> {
        return GlobalScope.async {
            val works = WorkCollection.instance.loadAllWorksInDate(date)
            val visits = VisitCollection.instance.loadVisitsByDate(date)
            val report = DailyReport(date, works, visits)
            report
        }
    }

    fun initAndSaveDailyReportAsync(date: Calendar): Deferred<Unit> {
        return GlobalScope.async {
            val report = initDailyReportAsync(date).await()
            setAsync(report)
            Unit
        }
    }

    suspend fun loadByDate(date: Calendar): DailyReport = suspendCoroutine {  cont ->

        GlobalScope.launch {

            val report1 = loadByDateCore(date)
            if (report1 != null) {
                cont.resume(report1)
            } else {

                val report2 = initDailyReportAsync(date).await()
                setAsync(report2)
                cont.resume(report2)
            }
        }
    }
}