package work.ckogyo.returnvisitor.models

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
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

    override fun initFromHashMap(map: HashMap<String, Any>) {
        super.initFromHashMap(map)

        address = map[addressKey].toString()
        latLng = LatLng(map[latitudeKey].toString().toDouble(), map[longitudeKey].toString().toDouble())
        category = Category.valueOf(map[categoryKey].toString())
        rating = Visit.Rating.valueOf(map[ratingKey].toString())

        if (category == Category.Room) {
            parentId = map[parentIdKey].toString()
        }
    }

    enum class Category{
        House, HousingComplex, Place, Room
    }

    var address = ""
    var latLng = LatLng(0.0, 0.0)
    var category = Category.House
    var rating = Visit.Rating.None

    // Roomだけ
    var parentId = ""


    override val hashMap: HashMap<String, Any>
        get() {
            val map = super.hashMap

            map[addressKey] = address
            map[latitudeKey] = latLng.latitude
            map[longitudeKey] = latLng.longitude
            map[categoryKey] = category
            map[ratingKey] = rating

            if (category == Category.Room) {
                map[parentIdKey] = parentId
            }

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


}