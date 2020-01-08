package work.ckogyo.returnvisitor.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import work.ckogyo.returnvisitor.models.Person
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.models.Work
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread

class FirebaseDB {

    companion object {
        private val _instance = FirebaseDB()
        val instance: FirebaseDB
            get() = _instance

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
    private fun loadList(collName: String, onFinished: (ArrayList<HashMap<String, Any>>) -> Unit) {
        val list = ArrayList<HashMap<String, Any>>()

        if (userDoc == null) {
            onFinished(list)
            return
        }

        userDoc!!.collection(collName).get().addOnSuccessListener {

            for (doc in it.documents) {
                list.add(doc.data as HashMap<String, Any>)
            }
            onFinished(list)

        }.addOnFailureListener {
            onFinished(list)
        }
    }

    fun loadPlaces(onFinished: (ArrayList<Place>) -> Unit) {
        loadList(placesKey){
            val places = ArrayList<Place>()
            for (map in it) {
                val place = Place()
                place.initFromHashMap(map)
                places.add(place)
            }
            onFinished(places)
        }
    }

    private fun loadListById(collName: String, id: String, onFinished: (ArrayList<HashMap<String, Any>>) -> Unit) {

        val list = ArrayList<HashMap<String, Any>>()

        if (userDoc == null) {
            onFinished(list)
            return
        }

        userDoc!!.collection(collName).whereEqualTo(idKey, id).get().addOnSuccessListener {

            for (doc in it.documents) {
                list.add(doc.data as HashMap<String, Any>)
            }
            onFinished(list)

        }.addOnFailureListener {
            onFinished(list)
        }
    }

    private fun loadById(collName: String, id: String, onFinished: ((HashMap<String, Any>?) -> Unit)) {
        loadListById(collName, id){
            if (it.isEmpty()) {
                onFinished(null)
            } else {
                onFinished(it[0])
            }
        }
    }

    fun loadPlaceById(id: String, onFinished: ((Place?) -> Unit)){
        loadById(placesKey, id){
            if (it == null) {
                onFinished(null)
            } else {
                val place = Place()
                place.initFromHashMap(it)
                onFinished(place)
            }
        }
    }

    fun loadPersonById(id: String, onFinished: ((Person?) -> Unit)){
        loadById(personsKey, id){
            if (it == null) {
                onFinished(null)
            } else {
                val person = Person()
                person.initFromHashMap(it)
                onFinished(person)
            }
        }
    }

    fun loadVisitsOfPlace(place: Place, onFinished: (ArrayList<Visit> ) -> Unit) {

        val visitsToPlace = ArrayList<Visit>()

        if (userDoc == null) {
            onFinished(visitsToPlace)
            return
        }

        userDoc!!.collection(visitsKey).whereEqualTo(placeIdKey, place.id).get().addOnSuccessListener {

            var docCount = 0
            if (it != null) {
                docCount += it.documents.size
                for (doc in it.documents) {
                    val v = Visit()
                    v.fromHashMap(doc.data as HashMap<String, Any>, this, place) {it2 ->
                        visitsToPlace.add(it2)
                        docCount--
                    }
                }
            }

            thread {
                while (docCount > 0) {
                    Thread.sleep(30)
                }
                onFinished(visitsToPlace)
            }
        }.addOnFailureListener {
            onFinished(visitsToPlace)
        }
    }

    fun loadLatestVisitToPlace(place: Place, onFinished: ((Visit?) -> Unit)) {
        loadVisitsOfPlace(place){
            when {
                it.isEmpty() -> onFinished(null)
                it.size == 1 -> onFinished(it[0])
                else -> {
                    val visit = it.sortedByDescending { v -> v.dateTime.timeInMillis }[0]
                    onFinished(visit)
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

    fun loadWorksOfDate(date: Calendar, onFinished: (ArrayList<Work>) -> Unit) {

        val works = ArrayList<Work>()

        if (userDoc == null) {
            onFinished(works)
        }

        val start = cloneDateWith0Time(date)
        val end = cloneDateWith0Time(date)
        end.add(Calendar.DATE, 1)

        userDoc!!.collection(worksKey)
            .whereGreaterThanOrEqualTo(startKey, start.timeInMillis)
//            .whereGreaterThanOrEqualTo(endKey, start.timeInMillis)
            .whereLessThan(startKey, end.timeInMillis)
//            .whereLessThan(endKey, end.timeInMillis)
            .get().addOnSuccessListener {

                for(doc in it.documents) {
                    val work = Work()
                    work.initFromHashMap(doc.data as HashMap<String, Any>)
                    works.add(work)
                    onFinished(works)
                }
            }.addOnFailureListener {
                onFinished(works)
            }
    }

}