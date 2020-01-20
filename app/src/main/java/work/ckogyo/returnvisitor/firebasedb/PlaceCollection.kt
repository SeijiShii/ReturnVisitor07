package work.ckogyo.returnvisitor.firebasedb

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.utils.categoryKey
import work.ckogyo.returnvisitor.utils.parentIdKey
import work.ckogyo.returnvisitor.utils.placesKey
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PlaceCollection {

    companion object {

        private val innerInstance = PlaceCollection()
        val instance: PlaceCollection
            get() = innerInstance
    }

    suspend fun loadPlacesForMap(): ArrayList<Place> = suspendCoroutine { cont ->
        GlobalScope.launch {
            val mapList = FirebaseDB.instance.loadList(placesKey)
            val places = ArrayList<Place>()
            for (map in mapList) {
                val place = Place()
                place.initFromHashMap(map)
                if (place.category != Place.Category.Room) {
                    places.add(place)
                }
            }
            cont.resume(places)
        }
    }

    suspend fun loadById(id: String): Place? = suspendCoroutine {
        GlobalScope.launch {
            val map = FirebaseDB.instance.loadById(placesKey, id)
            if (map == null) {
                it.resume(null)
            } else {
                val place = Place()
                place.initFromHashMap(map)
                it.resume(place)
            }
        }
    }

    suspend fun set(place: Place): Boolean = suspendCoroutine { cont ->
        GlobalScope.launch {
            cont.resume(FirebaseDB.instance.set(placesKey, place.id, place.hashMap))
        }
    }

    suspend fun delete(place: Place): Boolean = suspendCoroutine { cont ->
        GlobalScope.launch {
            cont.resume(FirebaseDB.instance.delete(placesKey, place.id))
        }
    }

    suspend fun loadRoomsByParentId(parentId: String): ArrayList<Place> = suspendCoroutine {  cont ->

        val rooms = ArrayList<Place>()

        val userDoc = FirebaseDB.instance.userDoc

        if (userDoc == null) {
            cont.resume(rooms)
        } else {
            userDoc.collection(placesKey)
                .whereEqualTo(parentIdKey, parentId)
                .whereEqualTo(categoryKey, Place.Category.Room)
                .get()
                .addOnSuccessListener {
                    for (doc in it.documents) {
                        val map = doc.data as HashMap<String, Any>
                        val room = Place()
                        room.initFromHashMap(map)
                        rooms.add(room)
                    }
                    rooms.sortBy { r -> r.name }
                    cont.resume(rooms)
                }
                .addOnFailureListener {
                    cont.resume(rooms)
                }
        }



    }




}