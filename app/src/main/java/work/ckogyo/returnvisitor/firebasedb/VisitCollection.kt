package work.ckogyo.returnvisitor.firebasedb

import android.util.Log
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.*
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.utils.DataModelKeys.dateTimeMillisKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.placeIdKey
import work.ckogyo.returnvisitor.utils.FirebaseCollectionKeys.infoTagIdsKey
import work.ckogyo.returnvisitor.utils.FirebaseCollectionKeys.placementIdsKey
import work.ckogyo.returnvisitor.utils.FirebaseCollectionKeys.visitsKey
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VisitCollection {

    companion object {
        private val innerInstance = VisitCollection()

        val instance: VisitCollection
            get() = innerInstance
    }

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
                    .whereEqualTo(placeIdKey, place.id)
                    .orderBy(dateTimeMillisKey, Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener {
                        if (it.documents.isEmpty()) {
                            cont.resume(null)
                        } else {
                            initVisitFromHashMap(it.documents[0].data as HashMap<String, Any>, cont)
                        }
                    }
                    .addOnFailureListener {
                        Log.d(debugTag, it.localizedMessage)
                        cont.resume(null)
                    }
            }
        }
    }

    private fun initVisitFromHashMap(map: HashMap<String, Any>, cont: Continuation<Visit>) {
        val visit = Visit()
        if (map.containsKey(placeIdKey)
            || map.containsKey(placementIdsKey)
            || map.containsKey(infoTagIdsKey)) {
            GlobalScope.launch {
                visit.initVisitFromHashMap(map)
                setAsync(visit)
                cont.resume(visit)
            }
        } else {
            visit.initFromHashMap(map)
            cont.resume(visit)
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


    private fun setAsync(visit: Visit): Deferred<Unit> {
        return GlobalScope.async{
            FirebaseDB.instance.set(visitsKey, visit.id, visit.hashMap)
            DailyReportCollection.instance.initAndSaveDailyReportAsync(visit.dateTime)
            Unit
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
            DailyReportCollection.instance.initAndSaveDailyReportAsync(visit.dateTime)
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

                        val visit = Visit()
                        visit.initFromHashMapSimple(doc.data as HashMap<String, Any>)
                        DailyReportCollection.instance.initAndSaveDailyReportAsync(visit.dateTime)
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
            PlaceCollection.instance.saveAsync(visit.place).await()

            for (person in visit.persons) {
                PersonCollection.instance.setAsync(person).await()
            }
        }
    }

    suspend fun addNotHomeVisitAsync(place: Place):Visit = suspendCoroutine { cont ->

//        val start = System.currentTimeMillis()

        GlobalScope.launch {
            val latestVisit = loadLatestVisitOfPlace(place)
            val visit = if (latestVisit == null) {
                val v = Visit()
                v.place = place
                v
            } else {
                Visit(latestVisit)
            }
            visit.turnToNotHome()
            saveVisitAsync(visit)

            MonthReportCollection.instance.updateByMonthAsync(visit.dateTime)
            DailyReportCollection.instance.initAndSaveDailyReportAsync(visit.dateTime)

//            Log.d(debugTag, "addNotHomeVisitAsync, took ${System.currentTimeMillis() - start}ms.")
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
                    GlobalScope.launch {
                        cont.resume(querySnapshotToVisitList(it))
                    }
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
                            val visit = Visit()
                            val map = it.documents[0].data as HashMap<String, Any>
                            GlobalScope.launch {
                                initVisitFromHashMap(map, cont)
//                                visit.initVisitFromHashMap(map)
//                                visit.initFromHashMap(map)
//                                cont.resume(visit)
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
//                            visit.initFromHashMapSimple(map)
//                            pairs.add(VisitMapPair(visit, map))
                            visits.add(visit)
                        }

                        // 場所に対する最新のVisitだけを取得
//                        val pairs2 = ArrayList<VisitMapPair>()

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

//                        for (pair in pairs) {
//                            var pairAlreadyContained: VisitMapPair? = null
//                            for (pair2 in pairs2) {
//
//                                if (pair.map.containsKey(placeIdKey)
//                                    && pair2.map.containsKey(placeIdKey)
//                                    && pair.map[placeIdKey] == pair2.map[placeIdKey]) {
//                                    pairAlreadyContained = pair2
//                                    break
//                                } else if (pair.visit.place == pair2.visit.place) {
//                                    pairAlreadyContained = pair2
//                                    break
//                                }
//                            }
//
//                            if (pairAlreadyContained != null) {
//                                if (pair.visit.dateTime.timeInMillis > pairAlreadyContained.visit.dateTime.timeInMillis) {
//                                    pairs2.remove(pairAlreadyContained)
//                                    pairs2.add(pair)
//                                }
//                            } else {
//                                pairs2.add(pair)
//                            }
//                        }

//                        val job = GlobalScope.launch initVisitsLaunch@ {
//
//                            var visits = ArrayList<Visit>()
//
//                            Log.d(debugTag, "pairs2.size: ${pairs2.size}")
//
//                            for (i in 0 until pairs2.size) {
//                                val visit = Visit()
//                                visit.initVisitFromHashMap(pairs2[i].map)
//                                setAsync(visit)
//
//                                Log.d(debugTag, "Visit initialized: ${visit.id}")
//
//                                visits.add(visit)
//
////                                Log.d(debugTag, "initVisitsLaunch@ isActive: $isActive")
//                                if (!isActive) {
//                                    return@initVisitsLaunch
//                                }
//
//                                if ((i > 0 && i % chunkSize == 0) || i >= pairs2.size - 1) {
//                                    chunkLoadedCallback(visits, pairs2.size)
//                                    visits = ArrayList()
//                                }
//                            }
//                        }
                        cont.resume(visits2)
                    }.addOnFailureListener {
                        cont.resume(ArrayList<Visit>())
                    }
            }
        }

    }

//    data class VisitMapPair(val visit: Visit, val map: HashMap<String, Any>)

    fun updatePlaceInVisitsAsync(place: Place): Deferred<Unit> {

        return GlobalScope.async {

            val visits = loadVisitsOfPlace(place)
            for (visit in visits) {
                visit.place = place
                setAsync(visit)
            }
        }
    }
}

