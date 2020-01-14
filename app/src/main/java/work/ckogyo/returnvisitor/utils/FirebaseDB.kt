package work.ckogyo.returnvisitor.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.models.Person
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.models.Work
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseDB {

    companion object {
        private val innerInstance = FirebaseDB()
        val instance: FirebaseDB
            get() = innerInstance

        private lateinit var auth: FirebaseAuth

        fun initialize(fAuth: FirebaseAuth) {
            auth = fAuth
        }
    }
    private val db = FirebaseFirestore.getInstance()

    private val userDoc: DocumentReference?
        get(){

            if (auth.currentUser == null) {
                Log.w(debugTag, "No user is logged into Firebase auth!")
                return null
            }

            val uid = auth.currentUser!!.uid
            return db.collection(uid).document(uid)
        }

    /**
     * コレクションの属するすべてをHashMapで取得
     */
    private fun loadListCallback(colName: String, onFinished: (ArrayList<HashMap<String, Any>>) -> Unit) {
        val list = ArrayList<HashMap<String, Any>>()

        if (userDoc == null) {
            onFinished(list)
            return
        }

        userDoc!!.collection(colName).get().addOnSuccessListener {

            for (doc in it.documents) {
                list.add(doc.data as HashMap<String, Any>)
            }
            onFinished(list)

        }.addOnFailureListener {
            onFinished(list)
        }
    }

    private suspend fun loadList(colName: String): ArrayList<HashMap<String, Any>> = suspendCoroutine { cont ->
        loadListCallback(colName){
            cont.resume(it)
        }
    }

    suspend fun loadPlaces(): ArrayList<Place> = suspendCoroutine { cont ->
        GlobalScope.launch {
            val mapList = loadList(placesKey)
            val places = ArrayList<Place>()
            for (map in mapList) {
                val place = Place()
                place.initFromHashMap(map)
                places.add(place)
            }
            cont.resume(places)
        }
    }

    private fun loadListByIdCallback(colName: String, id: String, onFinished: (ArrayList<HashMap<String, Any>>) -> Unit) {

        val list = ArrayList<HashMap<String, Any>>()

        if (userDoc == null) {
            onFinished(list)
            return
        }

        userDoc!!.collection(colName).whereEqualTo(idKey, id).get().addOnSuccessListener {

            for (doc in it.documents) {
                list.add(doc.data as HashMap<String, Any>)
            }
            onFinished(list)

        }.addOnFailureListener {
            onFinished(list)
        }
    }

    private fun loadByIdCallback(colName: String, id: String, onFinished: ((HashMap<String, Any>?) -> Unit)) {
        loadListByIdCallback(colName, id){
            if (it.isEmpty()) {
                onFinished(null)
            } else {
                onFinished(it[0])
            }
        }
    }

    private suspend fun loadById(colName: String, id: String): (HashMap<String, Any>)? = suspendCoroutine { cont ->
        loadByIdCallback(colName, id){
            cont.resume(it)
        }
    }

    suspend fun loadPlaceById(id: String): Place? = suspendCoroutine {
        GlobalScope.launch {
            val map = loadById(placesKey, id)
            if (map == null) {
                it.resume(null)
            } else {
                val place = Place()
                place.initFromHashMap(map)
                it.resume(place)
            }
        }
    }

    private fun loadPersonByIdCallback(id: String, onFinished: ((Person?) -> Unit)){
        loadByIdCallback(personsKey, id){
            if (it == null) {
                onFinished(null)
            } else {
                val person = Person()
                person.initFromHashMap(it)
                onFinished(person)
            }
        }
    }

    suspend fun loadPersonById(id: String): Person? = suspendCoroutine {  cont ->
        loadPersonByIdCallback(id) {
            cont.resume(it)
        }
    }


    suspend fun loadVisitsOfPlace(place: Place): ArrayList<Visit> = suspendCoroutine { cont ->

        var visitsToPlace = ArrayList<Visit>()

        if (userDoc == null) {
            cont.resume(visitsToPlace)
        }

        userDoc!!.collection(visitsKey).whereEqualTo(placeIdKey, place.id).get().addOnSuccessListener {
            GlobalScope.launch {
                visitsToPlace = querySnapshotToVisitList(it)
                cont.resume(visitsToPlace)
            }
        }.addOnFailureListener {
            cont.resume(visitsToPlace)
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

    private fun set(collName: String,
                    id:String, map:
                    HashMap<String, Any>,
                    onFinished: ((HashMap<String, Any>?) -> Unit)? = null) {

        if (userDoc == null) {
            onFinished?.invoke(null)
            return
        }

        userDoc!!.collection(collName).document(id).set(map).addOnSuccessListener {
            onFinished?.invoke(map)
        }.addOnFailureListener {
            onFinished?.invoke(null)
        }
    }

    fun setPlace(place: Place, onFinished: ((Place?) -> Unit)? = null) {
        set(placesKey, place.id, place.hashMap){
            if (it == null) {
                onFinished?.invoke(null)
            } else {
                onFinished?.invoke(place)
            }
        }
    }

    fun setVisit(visit: Visit, onFinished: ((Visit?) -> Unit)? = null) {
        set(visitsKey, visit.id, visit.hashMap){
            if (it == null) {
                onFinished?.invoke(null)
            } else {
                onFinished?.invoke(visit)
            }
        }
    }

    fun setPerson(person: Person, onFinished: ((Person?) -> Unit)? = null) {
        set(personsKey, person.id, person.hashMap){
            if (it == null) {
                onFinished?.invoke(null)
            } else {
                onFinished?.invoke(person)
            }
        }
    }

    fun setWork(work: Work, onFinished: ((Work?) -> Unit)? = null) {
        set(worksKey, work.id, work.hashMap){
            if (it == null) {
                onFinished?.invoke(null)
            } else {
                onFinished?.invoke(work)
            }
        }
    }

    private fun delete(collName: String,
                       id: String,
                       onFinished: ((Boolean) -> Unit)? = null) {
        if (userDoc == null) {
            onFinished?.invoke(false)
            return
        }

        userDoc!!.collection(collName).document(id).delete().addOnSuccessListener {
            onFinished?.invoke(true)
        }.addOnFailureListener {
            onFinished?.invoke(false)
        }
    }

    fun deletePlace(place: Place,
                            onFinished: ((Boolean) -> Unit)? = null) {
        delete(placesKey, place.id, onFinished)
    }

    fun deleteVisit(visit: Visit,
                            onFinished: ((Boolean) -> Unit)? = null) {
        delete(visitsKey, visit.id, onFinished)
    }

    fun deletePerson(person: Person,
                            onFinished: ((Boolean) -> Unit)? = null) {
        delete(personsKey, person.id, onFinished)
    }

    fun deleteVisitsToPlace(place: Place) {

        userDoc?:return

        userDoc!!.collection(visitsKey).whereEqualTo(placeIdKey, place.id).get().addOnSuccessListener {
            for (doc in it) {
                doc.reference.delete()
            }
        }
    }

    private fun loadWorksInDayByCallback(date: Calendar, byStart: Boolean, onLoaded: (ArrayList<Work>) -> Unit) {

        val works = ArrayList<Work>()

        if (userDoc == null) {
            return  onLoaded(works)
        }

        val startOfDay = cloneDateWith0Time(date)
        val endOfDay = cloneDateWith0Time(date)
        endOfDay.add(Calendar.DATE, 1)

        val key = if (byStart) startKey else endKey

        userDoc!!.collection(worksKey)
            .whereGreaterThanOrEqualTo(key, startOfDay.timeInMillis)
            .whereLessThan(key, endOfDay.timeInMillis)
            .get().addOnSuccessListener {

                for(doc in it.documents) {
                    val work = Work()
                    work.initFromHashMap(doc.data as HashMap<String, Any>)
                    works.add(work)
                }
                onLoaded(works)
            }.addOnFailureListener {
                onLoaded(works)
            }
    }

    // Coroutineの使い方はここが詳しい
    // https://qiita.com/k-kagurazaka@github/items/8595ca60a5c8d31bbe37#%E3%82%B3%E3%83%BC%E3%83%AB%E3%83%90%E3%83%83%E3%82%AF%E3%82%B9%E3%82%BF%E3%82%A4%E3%83%AB%E3%81%8B%E3%82%89%E4%B8%AD%E6%96%AD%E9%96%A2%E6%95%B0%E3%81%B8%E3%81%AE%E5%A4%89%E6%8F%9B

    private suspend fun loadWorksInDay(date: Calendar, byStart: Boolean): ArrayList<Work>
            = suspendCoroutine { cont ->
        loadWorksInDayByCallback(date, byStart){
            cont.resume(it)
        }
    }

    private suspend fun loadAllWorksInDay(date: Calendar): ArrayList<Work> {

        val worksByStart = loadWorksInDay(date, true)
        val worksByEnd = loadWorksInDay(date, false)

        worksByStart.addAll(worksByEnd)
        return filterUndupList(worksByStart)
    }

    suspend fun loadWorksByDateRange(startDate: Calendar, endDate: Calendar): ArrayList<Work> {
        val works = ArrayList<Work>()
        val dateCounter = startDate.clone() as Calendar

        while (isDateBefore(dateCounter, endDate)) {
            works.addAll(loadAllWorksInDay(dateCounter))
            dateCounter.add(Calendar.DAY_OF_MONTH, 1)
        }
        return filterUndupList(works)
    }

    private suspend fun querySnapshotToVisitList(qs: QuerySnapshot?): ArrayList<Visit> = suspendCoroutine{ cont ->

        val visits = ArrayList<Visit>()

        GlobalScope.launch {
            if (qs != null) {
                for (doc in qs.documents) {
                    val v = Visit().initFromHashMap(doc.data as HashMap<String, Any>, this@FirebaseDB)
                    visits.add(v)
                }
            }
            cont.resume(visits)
        }
    }

    private suspend fun loadVisitsOfDay(date: Calendar): ArrayList<Visit> = suspendCoroutine { cont ->

        val visits = ArrayList<Visit>()

        if (userDoc == null) {
            cont.resume(visits)
        }

        val startOfDay = cloneDateWith0Time(date)
        val endOfDay = cloneDateWith0Time(date)
        endOfDay.add(Calendar.DATE, 1)

        userDoc!!.collection(visitsKey)
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

    suspend fun loadVisitsByDateRange(startDate: Calendar, endDate: Calendar): ArrayList<Visit> {

        val visits = ArrayList<Visit>()
        val dateCounter = startDate.clone() as Calendar

        while (isDateBefore(dateCounter, endDate)) {
            visits.addAll(loadVisitsOfDay(dateCounter))
            dateCounter.add(Calendar.DAY_OF_MONTH, 1)
        }
        return filterUndupList(visits)
    }
}