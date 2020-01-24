package work.ckogyo.returnvisitor.firebasedb

import android.util.Log
import com.google.firebase.firestore.Query
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

    private suspend fun loadWorksByDate(date: Calendar, byStart: Boolean): ArrayList<Work> = suspendCoroutine { cont ->

        val db = FirebaseDB.instance
        val works = ArrayList<Work>()

        if (db.userDoc == null) {
            cont.resume(works)
        } else {
            val startOfDay = date.cloneWith0Time()
            val endOfDay = date.cloneWith0Time()
            endOfDay.add(Calendar.DATE, 1)
            endOfDay.add(Calendar.MILLISECOND, -1)

            val key = if (byStart) startKey else endKey

            db.userDoc!!.collection(worksKey)
                .whereGreaterThanOrEqualTo(key, startOfDay.timeInMillis)
                .whereLessThanOrEqualTo(key, endOfDay.timeInMillis)
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

    private suspend fun loadWorksByDateRange(start: Calendar, end: Calendar, byStart: Boolean): ArrayList<Work> = suspendCoroutine { cont ->

        val userDoc = FirebaseDB.instance.userDoc
        val works = ArrayList<Work>()

        if (userDoc == null) {
            cont.resume(works)
        } else {

            val startMillis = start.cloneWith0Time().timeInMillis
            val endLimit = end.cloneWith0Time()
            endLimit.add(Calendar.DAY_OF_MONTH, 1)
            val endMillis = endLimit.timeInMillis - 1

            val key = if (byStart) startKey else endKey

            userDoc.collection(worksKey)
                .whereGreaterThanOrEqualTo(key, startMillis)
                .whereLessThanOrEqualTo(key, endMillis)
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

    suspend fun loadWorksByDateRange(start: Calendar, end: Calendar): ArrayList<Work> {
        val worksByStart = loadWorksByDateRange(start, end, true)
        val worksByEnd = loadWorksByDateRange(start, end, false)

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

//        val start = System.currentTimeMillis()

        val userDoc = FirebaseDB.instance.userDoc
        if (userDoc == null) {
            cont.resume(null)
        } else {
            GlobalScope.launch {
                val direction = if (getFirst) Query.Direction.ASCENDING else Query.Direction.DESCENDING
                val key = if (getFirst) startKey else endKey
                val query = userDoc.collection(worksKey)
                    .orderBy(key, direction)
                    .limit(1)
                query.get().addOnSuccessListener {
                    if (it.documents.isNotEmpty()) {
                        val data = it.documents[0].data as HashMap<String, Any>
                        val work = Work()
                        work.initFromHashMap(data)
                        cont.resume(if (getFirst) work.start else work.end)
//                        Log.d(debugTag, "Work getRecordedDateAtEnd, took ${System.currentTimeMillis() - start}ms.")
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

            val startOfDay = date.cloneWith0Time()
            val endOfDay = date.cloneWith0Time()
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


    suspend fun set(work: Work): Boolean = suspendCoroutine { cont ->
        GlobalScope.launch {
            cont.resume(FirebaseDB.instance.set(worksKey, work.id, work.hashMap))
        }
    }

    /**
     * 指定した日時より前のWorkが存在するか。
     * Workの開始日時で判定する。
     */
    suspend fun hasWorkBeforeThan(dateTime: Calendar, includesEqual: Boolean = false): Boolean = suspendCoroutine { cont ->

//        val start = System.currentTimeMillis()

        val userDoc = FirebaseDB.instance.userDoc
        if (userDoc != null) {
            if (includesEqual) {
                userDoc.collection(worksKey)
                    .whereLessThanOrEqualTo(startKey, dateTime.timeInMillis)
            } else {
                userDoc.collection(worksKey)
                    .whereLessThan(startKey, dateTime.timeInMillis)
            }
                .limit(1)
                .addSnapshotListener { qs, _ ->
                    if (qs == null) {
                        cont.resume(false)
                    } else {
                        cont.resume(!qs.isEmpty)
                    }
//                    Log.d(debugTag, "hasWorkBeforeThan took ${System.currentTimeMillis() - start}ms.")
                }
        } else {
            cont.resume(false)
        }
    }

    /**
     * 指定した日時より後のWorkが存在するか。
     * Workの終了日時で判定する。
     */
    suspend fun hasWorkAfterThan(dateTime: Calendar, includesEqual: Boolean = false): Boolean = suspendCoroutine { cont ->

//        val start = System.currentTimeMillis()

        val userDoc = FirebaseDB.instance.userDoc
        if (userDoc != null) {
            if (includesEqual) {
                userDoc.collection(worksKey)
                    .whereGreaterThanOrEqualTo(startKey, dateTime.timeInMillis)
            } else {
                userDoc.collection(worksKey)
                    .whereGreaterThan(startKey, dateTime.timeInMillis)
            }
                .limit(1)
                .addSnapshotListener { qs, _ ->
                    if (qs == null) {
                        cont.resume(false)
                    } else {
                        cont.resume(!qs.isEmpty)
                    }
//                    Log.d(debugTag, "hasWorkAfterThan took ${System.currentTimeMillis() - start}ms.")
                }
        } else {
            cont.resume(false)
        }
    }

    private suspend fun hasWorkInDateTimeRange(start: Calendar, end: Calendar, byWorkStart: Boolean): Boolean = suspendCoroutine { cont ->

        val userDoc = FirebaseDB.instance.userDoc
        if (userDoc != null) {
            val key = if (byWorkStart) startKey else endKey
            userDoc.collection(worksKey)
                .whereGreaterThanOrEqualTo(key, start.timeInMillis)
                .whereLessThanOrEqualTo(key, end.timeInMillis)
                .limit(1)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot == null) {
                        cont.resume(false)
                    } else {
                        cont.resume(!snapshot.isEmpty)
                    }
                }
        } else {
            cont.resume(false)
        }
    }

    suspend fun hasWorkInDateTimeRange(start: Calendar, end: Calendar):Boolean = suspendCoroutine {  cont ->

//        val funStart  = System.currentTimeMillis()

        GlobalScope.launch {
            val byWorkStart  = hasWorkInDateTimeRange(start, end, true)
            val byWorkEnd = hasWorkInDateTimeRange(start, end, false)
            cont.resume(byWorkStart || byWorkEnd)

//            Log.d(debugTag, "hasWorkInDateTimeRange, took ${System.currentTimeMillis() - funStart}ms.")
        }
    }

}