package work.ckogyo.returnvisitor.firebasedb

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.models.*
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.utils.DataModelKeys.idKey
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseDB {

    companion object {
        private val innerInstance = FirebaseDB()
        val instance: FirebaseDB
            get() {
                return  innerInstance
            }

        private lateinit var auth: FirebaseAuth

        fun initialize(fAuth: FirebaseAuth) {
            auth = fAuth
        }
    }


    private val db = FirebaseFirestore.getInstance()

    private val placeColl = PlaceCollection()
    private val personColl = PersonCollection()
    private val visitColl = VisitCollection()
    private val workColl = WorkCollection()

    private val dReportColl = DailyReportCollection()
    private val mReportColl = MonthReportCollection()

    private val infoTagColl = InfoTagCollection()
    private val plcColl = PlacementCollection()

    init {
//        placeColl.copyParentNameToRoomAsync()
    }


    val userDoc: DocumentReference?
        get(){

            if (auth.currentUser == null) {
                Log.w(debugTag, "No user is logged into Firebase auth!")
                return null
            }

            val uid = auth.currentUser!!.uid
            return db.collection(uid).document(uid)
        }

    // Coroutineの使い方はここが詳しい
    // https://qiita.com/k-kagurazaka@github/items/8595ca60a5c8d31bbe37#%E3%82%B3%E3%83%BC%E3%83%AB%E3%83%90%E3%83%83%E3%82%AF%E3%82%B9%E3%82%BF%E3%82%A4%E3%83%AB%E3%81%8B%E3%82%89%E4%B8%AD%E6%96%AD%E9%96%A2%E6%95%B0%E3%81%B8%E3%81%AE%E5%A4%89%E6%8F%9B


    /**
     * コレクションの属するすべてをHashMapで取得
     */
    suspend fun loadList(colName: String): ArrayList<HashMap<String, Any>> = suspendCoroutine { cont ->

        GlobalScope.launch {
            val list = ArrayList<HashMap<String, Any>>()

            if (userDoc == null) {
                cont.resume(list)
            } else {
                userDoc!!.collection(colName).get().addOnSuccessListener {

                    for (doc in it.documents) {
                        list.add(doc.data as HashMap<String, Any>)
                    }
                    cont.resume(list)

                }.addOnFailureListener {
                    cont.resume(list)
                }
            }
        }
    }

    private suspend fun loadListById(collName: String, id: String): ArrayList<HashMap<String, Any>> = suspendCoroutine {  cont ->
        val list = ArrayList<HashMap<String, Any>>()

        if (userDoc == null) {
            cont.resume(list)
        } else {
            userDoc!!.collection(collName).whereEqualTo(idKey, id).get().addOnSuccessListener {

                for (doc in it.documents) {
                    list.add(doc.data as HashMap<String, Any>)
                }
                cont.resume(list)

            }.addOnFailureListener {
                cont.resume(list)
            }
        }
    }

    suspend fun loadById(collName: String, id: String): (HashMap<String, Any>)? = suspendCoroutine { cont ->

        GlobalScope.launch {
            val mapList = loadListById(collName, id)
            if (mapList.isEmpty()) {
                cont.resume(null)
            } else {
                cont.resume(mapList[0])
            }
        }
    }


    suspend fun set(collName: String,
                    id:String, map:
                    HashMap<String, Any>) = suspendCoroutine<Unit> {

        if (userDoc != null) {
            userDoc!!.collection(collName).document(id).set(map)
        }

        it.resume(Unit)
    }

    suspend fun delete(collName: String,
                       id: String): Boolean = suspendCoroutine { cont ->
        if (userDoc == null) {
            cont.resume(false)
        }

        userDoc!!.collection(collName).document(id).delete().addOnSuccessListener {
            cont.resume(true)
        }.addOnFailureListener {
            cont.resume(false)
        }
    }

    // Place関連

    suspend fun loadPlaceById(id: String): Place? = suspendCoroutine {  cont ->
        GlobalScope.launch {
            cont.resume(placeColl.loadById(id))
        }
    }

    suspend fun loadPlacesForMap():ArrayList<Place> = suspendCoroutine { cont ->
        GlobalScope.launch {
            val places = placeColl.loadPlacesForMap()
            cont.resume(places)
        }
    }

    suspend fun loadRoomsByParentId(parentId: String): ArrayList<Place> = suspendCoroutine { cont ->
        GlobalScope.launch {
            val rooms = placeColl.loadRoomsByParentId(parentId)
            cont.resume(rooms)
        }
    }

    /**
     * Placeを保存するときはそのPlaceを持つVisit内のPlaceもまとめて
     */
    fun savePlaceAsync(place: Place): Deferred<Unit> {

        return GlobalScope.async {
            refreshPlaceRatingAsync(place).await()
            placeColl.setAsync(place).await()
            visitColl.updatePlaceInVisitsAsync(place).await()

            GlobalScope.launch {
                // 保存する場所が部屋の場合
                if (place.category == Place.Category.Room) {
                    val hc = placeColl.loadById(place.parentId)
                    if (hc != null) {
                        refreshPlaceRatingAsync(hc).await()
                        placeColl.setAsync(hc)
                    }
                }
            }

            Unit
        }
    }

    fun deletePlaceAsync(place: Place): Deferred<Unit> {
        return GlobalScope.async {
            placeColl.deleteAsync(place).await()
            visitColl.deleteVisitsToPlace(place)
        }
    }

    suspend fun housingComplexHasRooms(hcId: String): Boolean = suspendCoroutine { cont ->
        GlobalScope.launch {
            cont.resume(placeColl.housingComplexHasRooms(hcId))
        }
    }

    fun refreshPlaceRatingAsync(place: Place):Deferred<Unit> {

        return GlobalScope.async {

            place.rating = Visit.Rating.Unoccupied

            if (place.category == Place.Category.HousingComplex) {
                val rooms = placeColl.loadRoomsByParentId(place.id)
                for (room in rooms) {
                    refreshPlaceRatingAsync(room).await()
                    if (room.rating.ordinal > place.rating.ordinal) {
                        place.rating = room.rating
                    }
                }
            } else {

                val visits = visitColl.loadVisitsOfPlace(place, limitLatest10 = false)

                if (visits.isNotEmpty()) {
                    visits.sortByDescending { v -> v.dateTime.timeInMillis }
                    for (v in visits) {
                        if (v.rating == Visit.Rating.Unoccupied || v.rating == Visit.Rating.NotHome) {
                            continue
                        }
                        place.rating = v.rating
                        break
                    }

                    if (place.rating == Visit.Rating.Unoccupied) {
                        place.rating = visits[0].rating
                    }
                }
            }
            Unit
        }
    }

    // Persons
    suspend fun loadPersonById(id: String) = personColl.loadById(id)

    // Visit関連

    fun saveVisitAsync(visit: Visit): Deferred<Unit> {

        return GlobalScope.async {
            visitColl.setAsync(visit).await()
            refreshPlaceRatingAsync(visit.place).await()
            placeColl.setAsync(visit.place).await()

            GlobalScope.launch {
                // Personが書き換わっていることもあるので
                for (pv in visit.personVisits) {
                    val visitsToPerson = visitColl.loadVisitsByPerson(pv.person)
                    for (visit2 in visitsToPerson) {
                        if (visit2 == visit) {
                            continue
                        }

                        visit2.replacePersonIfHas(pv.person)
                        visitColl.setAsync(visit2).await()
                    }
                }
            }

            GlobalScope.launch {
                // 保存する場所が部屋の場合
                if (visit.place.category == Place.Category.Room) {
                    val hc = placeColl.loadById(visit.place.parentId)
                    if (hc != null) {
                        refreshPlaceRatingAsync(hc).await()
                        placeColl.setAsync(hc)
                    }
                }
            }

            updateReports(visit.dateTime)

            Unit
        }
    }

    fun deleteVisitAsync(visit: Visit): Deferred<Unit> {

        return GlobalScope.async {
            visitColl.deleteAsync(visit).await()
            refreshPlaceRatingAsync(visit.place).await()
            placeColl.setAsync(visit.place)

            updateReports(visit.dateTime)
            Unit
        }
    }

    private fun updateReports(dateTime: Calendar) {
        GlobalScope.launch {
            dReportColl.initAndSaveDailyReportAsync(dateTime).await()
            mReportColl.updateByMonthAsync(dateTime).await()
        }
    }

    suspend fun loadLatestVisits(limitInAYear: Boolean = true,
                                 sortByDateTimeDescending: Boolean,
                                 sortByRatingDescending: Boolean): ArrayList<Visit>
            = visitColl.loadLatestVisits(limitInAYear, sortByDateTimeDescending, sortByRatingDescending)

    suspend fun prepareNextVisit(place: Place): Visit? = visitColl.prepareNextVisit(place)

    suspend fun loadVisitsToPlace(place: Place) = visitColl.loadVisitsOfPlace(place)

    suspend fun loadVisitsByDate(date: Calendar) = visitColl.loadVisitsByDate(date)

    suspend fun loadVisitsInMonth(month: Calendar) = visitColl.loadVisitsInMonth(month)

    suspend fun loadVisitsByDateRange(start: Calendar, end: Calendar) = visitColl.loadVisitsByDateRange(start, end)

    suspend fun generateNotHomeVisitAsync(place: Place) = visitColl.generateNotHomeVisitAsync(place)

    suspend fun getNeighboringDateWithVisitData(date: Calendar, before: Boolean) = visitColl.getNeighboringDateWithData(date, before)

    suspend fun hasVisitInDateTimeRange(start: Calendar, end: Calendar) = visitColl.hasVisitInDateTimeRange(start, end)

    suspend fun aDayHasVisit(date: Calendar) = visitColl.aDayHasVisit(date)

    // Works

    suspend fun loadWorkById(id: String) = workColl.loadById(id)

    fun saveWorkAsync(work: Work): Deferred<Unit> {

        return GlobalScope.async {
            workColl.setAsync(work).await()
            updateReports(work.start)
        }
    }

    suspend fun loadAllWorksInDate(date: Calendar) = workColl.loadAllWorksInDate(date)

    suspend fun loadWorksInMonth(month: Calendar) = workColl.loadWorkInMonth(month)

    suspend fun loadWorksByDateRange(start: Calendar, end: Calendar) = workColl.loadWorksByDateRange(start, end)

    fun deleteWorkAsync(work: Work): Deferred<Unit> {
        return GlobalScope.async {
            workColl.delete(work.id)
            updateReports(work.start)
            Unit
        }
    }

    suspend fun loadTotalDurationUntilLastMonth(month: Calendar) = workColl.loadTotalDurationUntilLastMonth(month)

    suspend fun getNeighboringDateWithWorkData(date: Calendar, before: Boolean) = workColl.getNeighboringDateWithData(date, before)

    suspend fun hasWorkInDateTimeRange(start: Calendar, end: Calendar) = workColl.hasWorkInDateTimeRange(start, end)

    suspend fun aDayHasWork(date: Calendar) = workColl.aDayHasWork(date)

    // InfoTags
    fun loadInfoTagsInLatestUseOrderAsync(): Deferred<ArrayList<InfoTag>> {
        return infoTagColl.loadInLatestUseOrderAsync()
    }

    fun saveInfoTagAsync(infoTag: InfoTag): Deferred<Unit> = infoTagColl.setAsync(infoTag)

    suspend fun loadInfoTagById(id: String) = infoTagColl.loadById(id)

    fun deleteInfoTagAsync(infoTag: InfoTag): Deferred<Unit> = infoTagColl.deleteAsync(infoTag)

    // Placements
    suspend fun loadPlacementsInLatestUseOrder() = plcColl.loadInLatestUseOrder()

    suspend fun loadPlacementById(id: String) = plcColl.loadById(id)

    fun savePlacementAsync(plc: Placement) = plcColl.setAsync(plc)

    fun deletePlacementAsync(plc: Placement) = plcColl.deleteAsync(plc)

    // MonthReports

    suspend fun loadMonthReport(month: Calendar): MonthReport = mReportColl.loadByMonth(month)


    // Utils
    suspend fun loadMonthList(): ArrayList<Calendar> = suspendCoroutine { cont ->

        GlobalScope.launch {

            val firstWork = workColl.loadWorkAtEnd(first = true)
            val lastWork = workColl.loadWorkAtEnd(first = false)

            val firstVisit = visitColl.loadVisitAtEnd(first = true)
            val lastVisit = visitColl.loadVisitAtEnd(first = false)

            val firstDate = when {
                firstWork == null && firstVisit == null -> null
                firstWork == null -> firstVisit!!.dateTime
                firstVisit == null -> firstWork.start
                else -> if (firstWork.start.isDateBefore(firstVisit.dateTime)) firstWork.start else firstVisit.dateTime
            }

            val lastDate = when {
                lastWork == null && lastVisit == null -> null
                lastWork == null -> lastVisit!!.dateTime
                lastVisit == null -> lastWork.start
                else -> if (lastWork.start.isDateBefore(lastVisit.dateTime)) lastWork.start else lastVisit.dateTime
            }

            val months = ArrayList<Calendar>()

            when {
                firstDate == null && lastDate == null -> cont.resume(months)
                firstDate == null -> {
                    val month = lastDate!!.clone() as Calendar
                    month.set(Calendar.DAY_OF_MONTH, 1)
                    months.add(month)
                }
                lastDate == null -> {
                    val month = firstDate.clone() as Calendar
                    month.set(Calendar.DAY_OF_MONTH, 1)
                    months.add(month)
                }
                else -> {

                    val counter = firstDate.clone() as Calendar
                    counter.set(Calendar.DAY_OF_MONTH, 1)

                    while (counter.isMonthBefore(lastDate, true)) {

                        val month = counter.clone() as Calendar
                        months.add(month)

                        counter.add(Calendar.MONTH, 1)
                    }
                }
            }
            cont.resume(months)
        }
    }
}