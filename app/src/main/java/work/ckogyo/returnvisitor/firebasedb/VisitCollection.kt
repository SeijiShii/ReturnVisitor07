package work.ckogyo.returnvisitor.firebasedb

import android.util.Log
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.*
import work.ckogyo.returnvisitor.models.Person
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.utils.DataModelKeys.dateTimeMillisKey
import work.ckogyo.returnvisitor.utils.FirebaseCollectionKeys.visitsKey
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VisitCollection {

//    companion object {
//        private val innerInstance = VisitCollection()
//
//        val instance: VisitCollection
//            get() = innerInstance
//    }

    suspend fun loadVisitsOfPlace(place: Place, limitLatest10: Boolean = false): ArrayList<Visit> = suspendCoroutine { cont ->

        val db = FirebaseDB.instance

        var visitsToPlace = ArrayList<Visit>()

        if (db.userDoc == null) {
            cont.resume(visitsToPlace)
        } else {
            val query = db.userDoc!!.collection(visitsKey)
                .get().addOnSuccessListener {

                    for (doc in it.documents) {
                        val visit = Visit().apply {
                            initFromHashMap(doc.data as HashMap<String, Any>)
                        }

                        if (visit.place == place) {
                            visitsToPlace.add(visit)
                        }
                    }

                    cont.resume(visitsToPlace)
                }
                .addOnFailureListener{
                    cont.resume(visitsToPlace)
                }
        }
    }

    private suspend fun loadLatestVisitOfPlace(place: Place): Visit? = suspendCoroutine { cont ->

//        val start = System.currentTimeMillis()

        GlobalScope.launch {

            val userDoc = FirebaseDB.instance.userDoc

            if (userDoc == null) {
                cont.resume(null)
            } else {

                userDoc.collection(visitsKey)
                    .get()
                    .addOnSuccessListener {
                        if (it.documents.isEmpty()) {
                            cont.resume(null)
                        } else {

                            var latestVisitToPlace: Visit? = null

                            for (doc in it.documents) {
                                val map = doc.data as HashMap<String, Any>
                                val visit = Visit()
                                visit.initFromHashMap(map)

                                if (visit.place == place) {
                                    if (latestVisitToPlace == null) {
                                        latestVisitToPlace = visit
                                    } else {
                                        if (visit.dateTime.timeInMillis > latestVisitToPlace.dateTime.timeInMillis) {
                                            latestVisitToPlace = visit
                                        }
                                    }
                                }
                            }
                            cont.resume(latestVisitToPlace)
                        }
                    }
                    .addOnFailureListener {
                        Log.d(debugTag, it.localizedMessage)
                        cont.resume(null)
                    }
            }
        }
    }

//    private fun initVisitFromHashMap(map: HashMap<String, Any>, cont: Continuation<Visit>) {
//        val visit = Visit()
//        if (map.containsKey(placeIdKey)
//            || map.containsKey(placementIdsKey)
//            || map.containsKey(infoTagIdsKey)) {
//            GlobalScope.launch {
//                visit.initVisitFromHashMap(map)
//                setAsync(visit)
//                cont.resume(visit)
//            }
//        } else {
//            visit.initFromHashMap(map)
//            cont.resume(visit)
//        }
//    }

    private fun querySnapshotToVisitList(qs: QuerySnapshot?): ArrayList<Visit> {

        val visits = ArrayList<Visit>()

        if (qs != null) {
            for (doc in qs.documents) {
                val visit = Visit()
                visit.initFromHashMap(doc.data as HashMap<String, Any>)
                visits.add(visit)
            }
        }
        return visits
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
                        val map = it.documents[0].data as HashMap<String, Any>
                        val simpleVisit = Visit().apply {
                            initFromHashMapSimple(map)
                        }
                        cont.resume(simpleVisit.dateTime)

                    } else {
                        cont.resume(null)
                    }
                }.addOnFailureListener {
                    cont.resume(null)
                }
            }
        }
    }


    fun setAsync(visit: Visit): Deferred<Unit> {
        return GlobalScope.async{
            FirebaseDB.instance.set(visitsKey, visit.id, visit.hashMap)
            Unit
        }
    }

    /**
     * Visitを削除し、Visitに属するPlaceのRatingを更新する。
     */
    fun deleteAsync(visit: Visit): Deferred<Unit> {
        return GlobalScope.async {
            FirebaseDB.instance.delete(visitsKey, visit.id)
            Unit
        }
    }

    /**
     * Placeを削除したときに呼ばれる
     */
    suspend fun deleteVisitsToPlace(place: Place) = suspendCoroutine<Unit> { cont ->

        val userDoc = FirebaseDB.instance.userDoc

        if (userDoc != null) {
            GlobalScope.launch {
                userDoc.collection(visitsKey).get().addOnSuccessListener {

                    for (doc in it.documents) {
                        val visit = Visit()
                        visit.initFromHashMap(doc.data as HashMap<String, Any>)

                        if (visit.place == place) {
                            FirebaseDB.instance.deleteVisitAsync(visit)
                        }
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

//    /**
//     * Visitを追加または更新し、Visitに属するPlaceのRatingを更新する。
//    */
//    fun saveVisitAsync(visit: Visit): Deferred<Unit> {
//        return GlobalScope.async {
//
//            setAsync(visit).await()
//            PlaceCollection.instance.saveAsync(visit.place).await()
//
//            for (person in visit.persons) {
//                PersonCollection.instance.setAsync(person).await()
//            }
//        }
//    }

    suspend fun generateNotHomeVisitAsync(place: Place):Visit = suspendCoroutine { cont ->

        GlobalScope.launch {
            val latestVisit = loadLatestVisitOfPlace(place)
            val visit = if (latestVisit == null) {
                val v = Visit()
                v.place = place
                v.rating = Visit.Rating.NotHome
                v
            } else {
                Visit(latestVisit)
            }
            visit.turnToNotHome()

            cont.resume(visit)
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

    suspend fun hasVisitInMonth(month: Calendar): Boolean = suspendCoroutine { cont ->

        val start = month.getStartOfMonth()
        val end = month.getEndOfMonth()

        GlobalScope.launch {
            cont.resume(hasVisitInDateTimeRange(start, end))
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
                    cont.resume(querySnapshotToVisitList(it))
                }.addOnFailureListener {
                    cont.resume(visits)
                }
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

            if (before) {
                userDoc.collection(visitsKey)
                    .whereLessThan(dateTimeMillisKey, dateStartMillis)
            } else {
                userDoc.collection(visitsKey)
                    .whereGreaterThan(dateTimeMillisKey, dateEndMillis)
            }
                .orderBy(dateTimeMillisKey, if (before) Query.Direction.DESCENDING else Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener {
                    if (it.documents.isEmpty()) {
                        cont.resume(null)
                    } else {
                        val map = it.documents[0].data as HashMap<String, Any>
                        val simpleVisit = Visit()
                        simpleVisit.initFromHashMapSimple(map)
                        cont.resume(simpleVisit.dateTime)
                    }
                }.addOnFailureListener {
                    cont.resume(null)
                }
        }
    }

    /**
     * 指定した場所に過去のVisitが存在すれば、最後のものを基準にVisitを準備する
     * なければnullを返す
     */
    suspend fun prepareNextVisit(place: Place): Visit? = suspendCoroutine {  cont ->
        GlobalScope.launch {
            val lastVisit = loadLatestVisitOfPlace(place)
            if (lastVisit == null) {
                cont.resume(null)
                return@launch
            }

            val visit = Visit(lastVisit)
            cont.resume(visit)
        }
    }

    suspend fun loadVisitAtEnd(first: Boolean): Visit? = suspendCoroutine { cont ->

        GlobalScope.launch {

            val userDoc = FirebaseDB.instance.userDoc
            if (userDoc == null) {
                cont.resume(null)
            } else {

                val direction = if (first) Query.Direction.ASCENDING else Query.Direction.DESCENDING

                userDoc.collection(visitsKey)
                    .orderBy(dateTimeMillisKey, direction)
                    .limit(1)
                    .get()
                    .addOnSuccessListener {
                        if (it.documents.isNotEmpty())  {
                            val map = it.documents[0].data as HashMap<String, Any>
                            GlobalScope.launch {
                                val visit = Visit()
                                visit.initFromHashMap(map)
                                cont.resume(visit)
                            }
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

    suspend fun loadLatestVisits(limitInAYear: Boolean = true,
                         sortByDateTimeDescending: Boolean,
                         sortByRatingDescending: Boolean): ArrayList<Visit>
            = suspendCoroutine { cont ->

        GlobalScope.launch {
            val userDoc = FirebaseDB.instance.userDoc

            if (userDoc == null) {
                cont.resume(ArrayList<Visit>())
            } else {

                val direction = if (sortByDateTimeDescending) Query.Direction.DESCENDING
                else Query.Direction.ASCENDING

                if (limitInAYear) {
                    val aYearAgo = Calendar.getInstance().cloneWith0Time()
                    aYearAgo.add(Calendar.YEAR, -1)
                    userDoc.collection(visitsKey).whereGreaterThan(dateTimeMillisKey, aYearAgo.timeInMillis)
                } else {
                    userDoc.collection(visitsKey)
                }.orderBy(dateTimeMillisKey, direction)
                    .get().addOnSuccessListener {

//                        val pairs = ArrayList<VisitMapPair>()
                        val visits = ArrayList<Visit>()

                        for (doc in it.documents) {
                            val map = doc.data as HashMap<String, Any>
                            val visit = Visit().apply {
                                initFromHashMap(map)
                            }
                            visits.add(visit)
                        }

                        // 場所に対する最新のVisitだけを取得

                        val visits2 = ArrayList<Visit>()

                        if (sortByDateTimeDescending) {
                            visits.sortByDescending { v -> v.dateTime.timeInMillis }
                        } else {
                            visits.sortBy { v -> v.dateTime.timeInMillis }
                        }

                        if (sortByRatingDescending) {
                            visits.sortByDescending { v -> v.rating.ordinal }
                        } else {
                            visits.sortBy { v -> v.rating.ordinal }
                        }

                        for (visit in visits) {
                            var visitOfPlaceContained: Visit? = null
                            for (visit2 in visits2) {
                                if (visit.place == visit2.place) {
                                    visitOfPlaceContained = visit2
                                }
                            }

                            if (visitOfPlaceContained != null) {
                                if (visit.dateTime.timeInMillis > visitOfPlaceContained.dateTime.timeInMillis) {
                                    visits2.remove(visitOfPlaceContained)
                                    visits2.add(visit)
                                }
                            } else {
                                visits2.add(visit)
                            }
                        }

                        cont.resume(visits2)
                    }.addOnFailureListener {
                        cont.resume(ArrayList<Visit>())
                    }
            }
        }

    }

    fun updatePlaceInVisitsAsync(place: Place): Deferred<Unit> {

        return GlobalScope.async {

            val visits = loadVisitsOfPlace(place)
            for (visit in visits) {
                visit.place = place
                setAsync(visit).await()
            }
        }
    }

    suspend fun loadVisitsInMonth(month: Calendar): ArrayList<Visit> = suspendCoroutine { cont ->

        GlobalScope.launch {
            val first = month.getFirstDay()
            val last = month.getLastDay()

            val visits = loadVisitsByDateRange(first, last)
            cont.resume(visits)
        }
    }

    suspend fun loadVisitsByPerson(person: Person): ArrayList<Visit> = suspendCoroutine {  cont ->

        val userDoc = FirebaseDB.instance.userDoc
        if (userDoc == null) {
            cont.resume(ArrayList())
        } else {
            userDoc.collection(visitsKey).get()
                .addOnSuccessListener {
                    val visitsToPerson = ArrayList<Visit>()
                    for (doc in it.documents) {
                        val map = doc.data as HashMap<String, Any>
                        val visit = Visit()
                        visit.initFromHashMap(map)
                        if (visit.hasPerson(person)) {
                            visitsToPerson.add(visit)
                        }
                    }
                    cont.resume(visitsToPerson)
                }
                .addOnFailureListener {
                    cont.resume(ArrayList())
                }
        }
    }

//    fun convertAllVisitsVerboseAsync(): Deferred<Unit> {
//        return GlobalScope.async {
//
//            FirebaseDB.instance.userDoc?.collection(visitsKey)?.get()
//                ?.addOnSuccessListener {
//                    for (doc in it.documents) {
//                        val map = doc.data as HashMap<String, Any>
//                        GlobalScope.launch {
//                            val visit = Visit()
//                            visit.initVisitFromHashMap(map)
//                            setAsync(visit)
//                        }
//                    }
//                }
//                ?.addOnFailureListener {
//
//                }
//            Unit
//        }
//    }
}

