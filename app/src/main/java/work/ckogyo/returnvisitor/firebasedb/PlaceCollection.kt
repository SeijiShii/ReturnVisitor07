package work.ckogyo.returnvisitor.firebasedb

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.DataModelKeys.categoryKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.parentIdKey
import work.ckogyo.returnvisitor.utils.FirebaseCollectionKeys.placesKey
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PlaceCollection {

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

    fun setAsync(place: Place): Deferred<Unit> {
        return GlobalScope.async {
            FirebaseDB.instance.set(placesKey, place.id, place.hashMap)
        }
    }

    /**
     * 集合住宅であれば部屋も削除する
     */
    fun deleteAsync(place: Place): Deferred<Boolean> {
        return GlobalScope.async {
            FirebaseDB.instance.delete(placesKey, place.id)
        }
    }

//    /**
//     * Ratingの更新も内部で行う
//     */
//    fun saveAsync(place: Place): Deferred<Unit> {
//        return GlobalScope.async {
//            place.refreshRatingByVisitsAsync().await()
//            setAsync(place).await()
//        }
//    }


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

    suspend fun housingComplexHasRooms(hcId: String): Boolean = suspendCoroutine { cont ->
        GlobalScope.launch {
            val rooms = loadRoomsByParentId(hcId)
            cont.resume(rooms.size > 0)
        }
    }

//    fun copyParentNameToRoomAsync(): Deferred<Unit> {
//        return GlobalScope.async {
//
//            FirebaseDB.instance.userDoc!!.collection(placesKey)
//                .get().addOnSuccessListener {
//                    for (doc in it.documents) {
//                        val place = Place()
//                        place.initFromHashMap(doc.data as HashMap<String, Any>)
//                        if (place.category == Place.Category.Room) {
//                            GlobalScope.launch {
//                                val parent = loadById(place.parentId)
//                                parent ?: return@launch
//
//                                if (parent.name != null && parent.name != "null") {
//                                    place.parentName = parent.name
//                                    FirebaseDB.instance.savePlaceAsync(place)
//                                }
//                            }
//
//                        }
//                    }
//                }
//            Unit
//        }
//    }

}