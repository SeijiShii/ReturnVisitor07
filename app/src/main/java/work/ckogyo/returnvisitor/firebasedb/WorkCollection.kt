package work.ckogyo.returnvisitor.firebasedb

import com.google.firebase.firestore.Query
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.models.Work
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.utils.DataModelKeys.endKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.startKey
import work.ckogyo.returnvisitor.utils.FirebaseCollectionKeys.worksKey
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
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


    suspend fun set(work: Work): Unit = suspendCoroutine {
        GlobalScope.launch {
            FirebaseDB.instance.set(worksKey, work.id, work.hashMap)
            it.resume(Unit)
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

    suspend fun getNeighboringDateWithData(date: Calendar, before: Boolean): Calendar? = suspendCoroutine {  cont ->

        val userDoc = FirebaseDB.instance.userDoc
        if (userDoc == null) {
            cont.resume(null)
        } else {

            val dateStartMillis = date.cloneWith0Time().timeInMillis
            val dateEnd = date.cloneWith0Time()
            dateEnd.add(Calendar.DAY_OF_MONTH, 1)
            dateEnd.add(Calendar.MILLISECOND, -1)
            val dateEndMillis = dateEnd.timeInMillis

            val key = if (before) startKey else endKey

            if (before) {
                userDoc.collection(worksKey)
                    .whereLessThan(key, dateStartMillis)
            } else {
                userDoc.collection(worksKey)
                    .whereGreaterThan(key, dateEndMillis)
            }
                .orderBy(key, if (before) Query.Direction.DESCENDING else Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener {
                    if (it.documents.isEmpty()) {
                        cont.resume(null)
                    } else {
                        val map = it.documents[0].data as HashMap<String, Any>
                        val work = Work()
                        work.initFromHashMap(map)
                        cont.resume(if (before) work.start else work.end)
                    }
                }.addOnFailureListener {
                    cont.resume(null)
                }
        }
    }

    suspend fun delete(id: String): Boolean = suspendCoroutine { cont ->
        GlobalScope.launch {
            cont.resume(FirebaseDB.instance.delete(worksKey, id))

        }
    }

    suspend fun loadWorkAtEnd(first: Boolean): Work? = suspendCoroutine { cont ->

        GlobalScope.launch {

            val userDoc = FirebaseDB.instance.userDoc
            if (userDoc == null) {
                cont.resume(null)
            } else {

                val direction = if (first) Query.Direction.ASCENDING else Query.Direction.DESCENDING

                userDoc.collection(worksKey)
                    .orderBy(startKey, direction)
                    .limit(1)
                    .get()
                    .addOnSuccessListener {
                        if (it.documents.isNotEmpty())  {
                            val work = Work()
                            val map = it.documents[0].data as HashMap<String, Any>
                            work.initFromHashMap(map)
                            cont.resume(work)
                        } else {
                            cont.resume(null)
                        }
                    }
                    .addOnFailureListener {
                        cont.resume(null)
                    }
            }
        }
    }

    private suspend fun loadDurationByRange(start: Calendar, end: Calendar): Long = suspendCoroutine { cont ->

        GlobalScope.launch {

            val userDoc = FirebaseDB.instance.userDoc
            if (userDoc == null) {
                cont.resume(0)
            } else {
                userDoc.collection(worksKey)
                    .whereGreaterThan(startKey, start.timeInMillis)
                    .whereLessThan(startKey, end.timeInMillis)
                    .get()
                    .addOnSuccessListener {
                        var sum  = 0L
                        for (doc in it.documents) {
                            val work = Work()
                            work.initFromHashMap(doc.data as HashMap<String, Any>)
                            sum += work.duration
                        }
                        cont.resume(sum)
                    }
                    .addOnFailureListener {
                        cont.resume(0)
                    }
            }
        }
    }

    suspend fun loadTotalDurationUntilLastMonth(month: Calendar): Long = suspendCoroutine { cont ->

        GlobalScope.launch {

            val firstWork = loadWorkAtEnd(first = true)
            if (firstWork == null) {
                cont.resume(0)
                return@launch
            }

            val start = firstWork.start.cloneWith0Time()

            val end = month.cloneWith0Time()
            end.set(Calendar.DAY_OF_MONTH, 1)
            end.add(Calendar.MILLISECOND, -1)

            val dur = loadDurationByRange(start, end)
            cont.resume(dur)

        }
    }

}