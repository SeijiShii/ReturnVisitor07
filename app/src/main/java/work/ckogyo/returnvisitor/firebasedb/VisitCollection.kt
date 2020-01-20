package work.ckogyo.returnvisitor.firebasedb

import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.models.Work
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

        val db = FirebaseDB.instance
        val visits = ArrayList<Visit>()

        if (db.userDoc == null) {
            cont.resume(visits)
        } else {
            val startOfDay = cloneDateWith0Time(date)
            val endOfDay = cloneDateWith0Time(date)
            endOfDay.add(Calendar.DATE, 1)

            db.userDoc!!.collection(visitsKey)
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
            val startOfDay = cloneDateWith0Time(date)
            val endOfDay = cloneDateWith0Time(date)
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


//    suspend fun loadVisitsByDateRange(startDate: Calendar, endDate: Calendar): ArrayList<Visit> {
//
//        val visits = ArrayList<Visit>()
//        val dateCounter = startDate.clone() as Calendar
//
//        while (isDateBefore(
//                dateCounter,
//                endDate
//            )
//        ) {
//            visits.addAll(loadVisitsOfDay(dateCounter))
//            dateCounter.add(Calendar.DAY_OF_MONTH, 1)
//        }
//        return filterUndupList(visits)
//    }


    suspend fun set(visit: Visit): Boolean = suspendCoroutine { cont ->
        GlobalScope.launch {
            cont.resume(FirebaseDB.instance.set(visitsKey, visit.id, visit.hashMap))
        }
    }

    suspend fun delete(visit: Visit): Boolean  = suspendCoroutine{ cont ->
        GlobalScope.launch {
            cont.resume(FirebaseDB.instance.delete(visitsKey, visit.id))
        }
    }

    suspend fun deleteVisitsToPlace(place: Place): Boolean = suspendCoroutine { cont ->

        val db = FirebaseDB.instance

        if (db.userDoc == null) {
            cont.resume(false)
        } else {
            GlobalScope.launch {
                db.userDoc!!.collection(visitsKey).whereEqualTo(
                    placeIdKey, place.id).get().addOnSuccessListener {
                    for (doc in it) {
                        doc.reference.delete()
                        cont.resume(true)
                    }
                }.addOnFailureListener {
                    cont.resume(false)
                }
            }
        }
    }

    fun saveVisit(visit: Visit): Deferred<Unit> {
        return GlobalScope.async {

            val placeColl = PlaceCollection.instance
            val visitsOfPlace = loadVisitsOfPlace(visit.place)
            visitsOfPlace.add(visit)
            visit.place.refreshRatingByVisits(visitsOfPlace)

            set(visit)
            placeColl.set(visit.place)

            for (person in visit.persons) {
                PersonCollection.instance.set(person)
            }
        }
    }




}