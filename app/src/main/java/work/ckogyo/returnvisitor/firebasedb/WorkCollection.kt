package work.ckogyo.returnvisitor.firebasedb

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.models.Work
import work.ckogyo.returnvisitor.utils.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WorkCollection {

    companion object {
        private val innerInstance = WorkCollection()
        val instance: WorkCollection
            get() = innerInstance
    }

    private suspend fun loadWorksInDay(date: Calendar, byStart: Boolean): ArrayList<Work>
            = suspendCoroutine { cont ->

        val db = FirebaseDB.instance
        val works = ArrayList<Work>()

        if (db.userDoc == null) {
            cont.resume(works)
        }

        val startOfDay = cloneDateWith0Time(date)
        val endOfDay = cloneDateWith0Time(date)
        endOfDay.add(Calendar.DATE, 1)

        val key = if (byStart) startKey else endKey

        db.userDoc!!.collection(worksKey)
            .whereGreaterThanOrEqualTo(key, startOfDay.timeInMillis)
            .whereLessThan(key, endOfDay.timeInMillis)
            .get().addOnSuccessListener {

                for(doc in it.documents) {
                    val work = Work()
                    work.initFromHashMap(doc.data as HashMap<String, Any>)
                    works.add(work)
                }
                cont.resume(works)
            }.addOnFailureListener {
                cont.resume(works)
            }
    }

    suspend fun loadWorksByDate(date: Calendar): ArrayList<Work> {

        val worksByStart = loadWorksInDay(date, true)
        val worksByEnd = loadWorksInDay(date, false)

        worksByStart.addAll(worksByEnd)
        return filterUndupList(worksByStart)
    }

//    suspend fun loadWorksByDateRange(startDate: Calendar, endDate: Calendar): ArrayList<Work> {
//        val works = ArrayList<Work>()
//        val dateCounter = startDate.clone() as Calendar
//
//        while (isDateBefore(
//                dateCounter,
//                endDate
//            )
//        ) {
//            works.addAll(loadAllWorksInDay(dateCounter))
//            dateCounter.add(Calendar.DAY_OF_MONTH, 1)
//        }
//        return filterUndupList(works)
//    }

    suspend fun set(work: Work): Boolean = suspendCoroutine { cont ->
        GlobalScope.launch {
            cont.resume(FirebaseDB.instance.set(worksKey, work.id, work.hashMap))
        }
    }

}