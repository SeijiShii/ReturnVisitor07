package work.ckogyo.returnvisitor.models

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.utils.*

class Place : BaseDataModel{

    companion object{
        const val idPrefix = "place"
    }

    constructor():super(idPrefix)

    constructor(o: JSONObject):super(o){

        address = o.optString(addressKey)
        latLng = LatLng(o.optDouble(latitudeKey), o.optDouble(longitudeKey))

        if (o.has(categoryKey)) {
            category = Category.valueOf(o.getString(categoryKey))
        }
    }

//    constructor(map: HashMap<String, Any>):super(map) {
//
//        address = map[addressKey].toString()
//        latLng = LatLng(map[latitudeKey].toString().toDouble(), map[longitudeKey].toString().toDouble())
//        category = Category.valueOf(map[categoryKey].toString())
//        rating = Visit.Rating.valueOf(map[ratingKey].toString())
//
////        val visitsMapList = map[visitsKey] as ArrayList<HashMap<String, Any>>
////        for (vm in visitsMapList) {
////            visits.add(Visit(vm))
////        }
//    }

    override fun initFromHashMap(map: HashMap<String, Any>) {
        super.initFromHashMap(map)

        address = map[addressKey].toString()
        latLng = LatLng(map[latitudeKey].toString().toDouble(), map[longitudeKey].toString().toDouble())
        category = Category.valueOf(map[categoryKey].toString())
        rating = Visit.Rating.valueOf(map[ratingKey].toString())
    }

//    fun addOrUpdateVisit(visit: Visit) {
//
//        val v = getVisitById(visit.id)
//        if (v != null) {
//            visits.remove(v)
//        }
//
//        visits.add(visit)
//    }

//    private fun getVisitById(visitId: String): Visit? {
//        for (v in visits) {
//            if (v.id == visitId) {
//                return v
//            }
//        }
//        return null
//    }

    enum class Category{
        House, HousingComplex, Place
    }

    var address = ""
    var latLng = LatLng(0.0, 0.0)
    var category = Category.House
    var rating = Visit.Rating.None

//    val visits = ArrayList<Visit>()

//    override val jsonObject: JSONObject
//    get() {
//        val o = super.jsonObject
//
//        o.put(addressKey, address)
//        o.put(latitudeKey, latLng.latitude)
//        o.put(longitudeKey, latLng.longitude)
//        o.put(categoryKey, category.toString())
//
//        return o
//    }

    override val hashMap: HashMap<String, Any>
        get() {
            val map = super.hashMap

            map[addressKey] = address
            map[latitudeKey] = latLng.latitude
            map[longitudeKey] = latLng.longitude
            map[categoryKey] = category
            map[ratingKey] = rating

//            val visitMapList = ArrayList<HashMap<String, Any>>()
//            for (v in visits) {
//                visitMapList.add(v.hashMap)
//            }
//            map[visitsKey] = visitMapList

            return map
        }

    override fun clone(): Place {
        val cloned = Place()
        super.cloneBaseProperties(cloned)

        cloned.address = address
        cloned.latLng = LatLng(latLng.latitude, latLng.longitude)
        cloned.category = category

        return cloned
    }

    fun toString(context: Context): String {
        if (address.isNotEmpty()) return address
        return "${context.getString(R.string.latitude)}: ${latLng.latitude}, ${context.getString(R.string.longitude)}: ${latLng.longitude}"
    }

    fun refreshRatingByVisits(visitsToPlace: ArrayList<Visit>) {

        if (visitsToPlace.isEmpty()) {
            rating = Visit.Rating.None
            return
        }

        if (visitsToPlace.size == 1) {
            rating = visitsToPlace[0].rating
            return
        }

        val visits = visitsToPlace.sortedByDescending { v -> v.dateTime.timeInMillis }

        for (v in visits) {
            if (v.rating == Visit.Rating.None || v.rating == Visit.Rating.NotHome) {
                continue
            }
            rating = v.rating
            break
        }

        if (rating == Visit.Rating.None) {
            rating = visitsToPlace[0].rating
        }

    }

//    fun refreshRating(userDoc: DocumentReference, onFinished:(Place) -> Unit) {
//
//        userDoc.collection(visitsKey).whereEqualTo(placeIdKey, id).addSnapshotListener { snapshot, e ->
//            val visits = ArrayList<Visit>()
//            if (e != null) {
//                if (snapshot != null) {
//
//                    for (doc in snapshot.documents) {
//                        val v = Visit()
//                        v.fromHashMap(doc.data as HashMap<String, Any>, this)
//                        visits.add(v)
//                    }
//                }
//            }
//            rating = if (visits.isEmpty()) {
//                Visit.Rating.None
//            } else {
//                visits.maxBy { v -> v.dateTime.timeInMillis }?.rating?:Visit.Rating.None
//            }
//            onFinished(this)
//        }
//    }

}