package work.ckogyo.returnvisitor.firebasedb

import android.util.Log
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VisitCollection {

    companion object {
        private val innerInstance = VisitCollection()

        val instance: VisitCollection
            get() = innerInstance
    }

    suspend fun loadVisitsOfPlace(place: Place): ArrayList<Visit> = suspendCoroutine { cont ->

        val db = FirebaseDB.instance

        var visitsToPlace = ArrayList<Visit>()

        if (db.userDoc == null) {
            cont.resume(visitsToPlace)
        } else {
            db.userDoc!!.collection(visitsKey).whereEqualTo(
                placeIdKey, place.id).get().addOnSuccessListener {
                GlobalScope.launch {
                    visitsToPlace = querySnapshotToVisitList(it)
                    cont.resume(visitsToPlace)
                }
            }.addOnFailureListener {
                cont.resume(visitsToPlace)
            }
        }
    }

    suspend fun loadLatestVisitOfPlace(place: Place): Visit? = suspendCoroutine { cont ->

        GlobalScope.launch {
            val visits = loadVisitsOfPlace(place)
            when {
                visits.isEmpty() -> cont.resume(null)
                else -> {
                    val visit = visits.sortedByDescending { v -> v.dateTime.timeInMillis }[0]
                    cont.resume(visit)
                }
            }
        }
    }

    private suspend fun querySnapshotToVisitList(qs: QuerySnapshot?): ArrayList<Visit> = suspendCoroutine{ cont ->

        val visits = ArrayList<Visit>()

        GlobalScope.launch {
            if (qs != null) {
                for (doc in qs.documents) {
                    val v = Visit().initVisitFromHashMap(doc.data as HashMap<String, Any>)
                    visits.add(v)
                }
            }
            cont.resume(visits)
        }
    }

    suspend fun loadVisitsByDate(date: Calendar): ArrayList<Visit> = suspendCoroutine { cont ->

        val userDoc = FirebaseDB.instance.userDoc
        val visits = ArrayList<Visit>()

        if (userDoc == null) {
            cont.resume(visits)
        } else {
            val startOfDay = date.cloneWith0Time()
            val endOfDay = date.cloneWith0Time()
            endOfDay.add(Calendar.DATE, 1)
            endOfDay.add(Calendar.MILLISECOND, -1)

            userDoc.collection(visitsKey)
                .whereGreaterThanOrEqualTo(dateTimeMillisKey, startOfDay.timeInMillis)
                .whereLessThan(dateTimeMillisKey, endOfDay.timeInMillis)
                .get().addOnSuccessListener {
                    GlobalScope.launch {
                        cont.resume(querySnapshotToVisitList(it))
                    }
                }.addOnFailureListener {
                    cont.resume(visits)
                }
        }
    }

    suspend fun aDayHasVisit(date: Calendar):Boolean = suspendCoroutine { cont ->

        val db = FirebaseDB.instance
        if (db.userDoc == null) {
            cont.resume(false)
        } else {
            val startOfDay = date.cloneWith0Time()
            val endOfDay = date.cloneWith0Time()
            endOfDay.add(Calendar.DATE, 1)

            db.userDoc!!.collection(visitsKey)
                .whereGreaterThanOrEqualTo(dateTimeMillisKey, startOfDay.timeInMillis)
                .whereLessThan(dateTimeMillisKey, endOfDay.timeInMillis)
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

    suspend fun getRecordedDateAtEnd(getFirst: Boolean): Calendar? = suspendCoroutine { cont ->

//        val start = System.currentTimeMillis()

        val db = FirebaseDB.instance
        if (db.userDoc == null) {
            cont.resume(null)
        } else {
            GlobalScope.launch {
                val direction = if (getFirst) Query.Direction.ASCENDING else Query.Direction.DESCENDING
                val query = db.userDoc!!.collection(visitsKey)
                    .orderBy(dateTimeMillisKey, direction)
                    .limit(1)
                query.get().addOnSuccessListener {
                    if (it.documents.size > 0) {
                        val data = it.documents[0].data as HashMap<String, Any>
                        val visit = Visit()
                        GlobalScope.launch {
                            visit.initVisitFromHashMap(data)
                            cont.resume(visit.dateTime)
//                            Log.d(debugTag, "Visit getRecordedDateAtEnd, took ${System.currentTimeMillis() - start}ms.")
                        }
                    } else {
                        cont.resume(null)
                    }
                }.addOnFailureListener {
                    cont.resume(null)
                }
            }
        }
    }


    private fun setAsync(visit: Visit): Deferred<Boolean> {
        return GlobalScope.async{
            FirebaseDB.instance.set(visitsKey, visit.id, visit.hashMap)
        }
    }

    /**
     * Visitを削除し、Visitに属するPlaceのRatingを更新する。
     */
    fun deleteAsync(visit: Visit): Deferred<Boolean> {
        return GlobalScope.async {
            val result = FirebaseDB.instance.delete(visitsKey, visit.id)
            visit.place.refreshRatingByVisitsAsync().await()
            PlaceCollection.instance.saveAsync(visit.place).await()
            result
        }
    }

    /**
     * Placeを削除したときに呼ばれる
     */
    suspend fun deleteVisitsToPlace(place: Place) = suspendCoroutine<Unit> { cont ->

        val db = FirebaseDB.instance

        if (db.userDoc != null) {
            GlobalScope.launch {
                db.userDoc!!.collection(visitsKey).whereEqualTo(
                    placeIdKey, place.id).get().addOnSuccessListener {
                    for (doc in it) {
                        doc.reference.delete()
                    }
                    cont.resume(Unit)
                }.addOnFailureListener {
                    cont.resume(Unit)
                }
            }
        } else {
            cont.resume(Unit)
        }
    }

    /**
     * Visitを追加または更新し、Visitに属するPlaceのRatingを更新する。
    */
    fun saveVisitAsync(visit: Visit): Deferred<Unit> {
        return GlobalScope.async {

            setAsync(visit).await()
            visit.place.refreshRatingByVisitsAsync().await()
            PlaceCollection.instance.saveAsync(visit.place).await()

            for (person in visit.persons) {
                PersonCollection.instance.setAsync(person).await()
            }
        }
    }

    fun addNotHomeVisitAsync(place: Place):Deferred<Visit> {

        return GlobalScope.async {
            val latestVisit = loadLatestVisitOfPlace(place)
            val visit = if (latestVisit == null) {
                val v = Visit()
                v.place = place
                v
            } else {
                Visit(latestVisit)
            }
            visit.turnToNotHome()
            saveVisitAsync(visit).await()
            visit
        }
    }

    suspend fun hasVisitBeforeThan(dateTime: Calendar, includesEqual: Boolean = false): Boolean = suspendCoroutine { cont ->
//        val start = System.currentTimeMillis()

        val userDoc = FirebaseDB.instance.userDoc
        if (userDoc != null) {
            val query = if (includesEqual) {
                userDoc.collection(visitsKey)
                    .whereLessThanOrEqualTo(dateTimeMillisKey, dateTime.timeInMillis)
            } else {
                userDoc.collection(visitsKey)
                    .whereLessThan(dateTimeMillisKey, dateTime.timeInMillis)
            }
            query.limit(1)
                .addSnapshotListener{ qs, _ ->
                    if (qs == null) {
                        cont.resume(false)
                    } else {
                        cont.resume(!qs.isEmpty)
                    }
//                    Log.d(debugTag, "hasVisitBeforeThan took ${System.currentTimeMillis() - start}ms.")
                }
        } else {
            cont.resume(false)
        }
    }

    suspend fun hasVisitAfterThan(dateTime: Calendar, includesEqual: Boolean = false): Boolean = suspendCoroutine { cont ->

//        val start = System.currentTimeMillis()

        val userDoc = FirebaseDB.instance.userDoc
        if (userDoc != null) {
            val query = if (includesEqual) {
                userDoc.collection(visitsKey)
                    .whereGreaterThanOrEqualTo(dateTimeMillisKey, dateTime.timeInMillis)
            } else {
                userDoc.collection(visitsKey)
                    .whereGreaterThan(dateTimeMillisKey, dateTime.timeInMillis)
            }
            query.limit(1)
                .addSnapshotListener{ qs, _ ->
                    if (qs == null) {
                        cont.resume(false)
                    } else {
                        cont.resume(!qs.isEmpty)
                    }
//                    Log.d(debugTag, "hasVisitBeforeThan took ${System.currentTimeMillis() - start}ms.")
                }
        } else {
            cont.resume(false)
        }
    }

    suspend fun hasVisitInDateTimeRange(start: Calendar, end: Calendar): Boolean = suspendCoroutine { cont ->

//        val funStart = System.currentTimeMillis()

        val userDoc = FirebaseDB.instance.userDoc
        if (userDoc != null) {
            userDoc.collection(visitsKey)
                .whereGreaterThanOrEqualTo(dateTimeMillisKey, start.timeInMillis)
                .whereLessThanOrEqualTo(dateTimeMillisKey, end.timeInMillis)
                .limit(1)
                .addSnapshotListener{ qs, _ ->
                    if (qs == null) {
                        cont.resume(false)
                    } else {
                        cont.resume(!qs.isEmpty)
                    }
//                    Log.d(debugTag, "hasVisitInDateTimeRange, took ${System.currentTimeMillis() - funStart}ms.")
                }
        } else {
            cont.resume(false)
        }
    }

    suspend fun loadVisitsByDateRange(start: Calendar, end: Calendar): ArrayList<Visit> = suspendCoroutine {  cont ->

        val userDoc = FirebaseDB.instance.userDoc
        val visits = ArrayList<Visit>()

        if (userDoc == null) {
            cont.resume(visits)
        } else {
            val startMillis = start.cloneWith0Time().timeInMillis
            val endLimit = end.cloneWith0Time()
            endLimit.add(Calendar.DAY_OF_MONTH, 1)
            val endMillis = endLimit.timeInMillis - 1

            userDoc.collection(visitsKey)
                .whereGreaterThanOrEqualTo(dateTimeMillisKey, startMillis)
                .whereLessThanOrEqualTo(dateTimeMillisKey, endMillis)
                .get().addOnSuccessListener {
                    GlobalScope.launch {
                        cont.resume(querySnapshotToVisitList(it))
                    }
                }.addOnFailureListener {
                    cont.resume(visits)
                }
        }
    }

}