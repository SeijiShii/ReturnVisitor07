package work.ckogyo.returnvisitor.firebasedb

import com.google.firebase.firestore.Query
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.models.Visit
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

    private suspend fun loadWorksByDate(date: Calendar, byStart: Boolean): ArrayList<Work> = suspendCoroutine { cont ->

        val db = FirebaseDB.instance
        val works = ArrayList<Work>()

        if (db.userDoc == null) {
            cont.resume(works)
        } else {
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
    }

    suspend fun loadAllWorksInDate(date: Calendar): ArrayList<Work> {

        val worksByStart = loadWorksByDate(date, true)
        val worksByEnd = loadWorksByDate(date, false)

        worksByStart.addAll(worksByEnd)
        return filterUndupList(worksByStart)
    }

    suspend fun aDayHasWork(date: Calendar): Boolean = suspendCoroutine {  cont ->

        val db = FirebaseDB.instance
        if (db.userDoc == null) {
            cont.resume(false)
        } else {

            GlobalScope.launch {
                when {
                    hasWorkInDateBy(true, date) -> cont.resume(true)
                    hasWorkInDateBy(false, date) -> cont.resume(true)
                    else -> cont.resume(false)
                }
            }
        }
    }

    suspend fun getRecordedDateAtEnd(getFirst: Boolean): Calendar? = suspendCoroutine { cont ->

        val db = FirebaseDB.instance
        if (db.userDoc == null) {
            cont.resume(null)
        } else {
            GlobalScope.launch {
                val direction = if (getFirst) Query.Direction.ASCENDING else Query.Direction.DESCENDING
                val key = if (getFirst) startKey else endKey
                val query = db.userDoc!!.collection(worksKey)
                    .orderBy(key, direction)
                    .limit(1)
                query.get().addOnSuccessListener {
                    if (it.documents.size > 0) {
                        val data = it.documents[0].data as HashMap<String, Any>
                        val work = Work()
                        work.initFromHashMap(data)
                        cont.resume(work.start)
                    } else {
                        cont.resume(null)
                    }
                }.addOnFailureListener {
                    cont.resume(null)
                }
            }
        }
    }

    private suspend fun hasWorkInDateBy(byStart: Boolean, date: Calendar): Boolean = suspendCoroutine { cont ->
        val db = FirebaseDB.instance
        if (db.userDoc == null) {
            cont.resume(false)
        } else {

            val startOfDay = cloneDateWith0Time(date)
            val endOfDay = cloneDateWith0Time(date)
            endOfDay.add(Calendar.DATE, 1)

            val key = if (byStart) startKey else endKey

            GlobalScope.launch {
                db.userDoc!!.collection(worksKey)
                    .whereGreaterThanOrEqualTo(key, startOfDay.timeInMillis)
                    .whereLessThan(key, endOfDay.timeInMillis)
                    .get().addOnSuccessListener {
                        if (it.documents.size > 0) {
                            cont.resume(true)
                        } else {
                            cont.resume(false)
                        }
                    }.addOnFailureListener {
                        cont.resume(false)
                    }
            }
        }
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