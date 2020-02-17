package work.ckogyo.returnvisitor.models

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.json.JSONObject
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.PlaceCollection
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.utils.*
import java.lang.StringBuilder

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
    var rating = Visit.Rating.Unoccupied

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

    fun refreshRatingByVisitsAsync():Deferred<Unit> {

        return GlobalScope.async {

            rating = Visit.Rating.Unoccupied

            if (category == Category.HousingComplex) {
                val rooms = PlaceCollection.instance.loadRoomsByParentId(id)
                for (room in rooms) {
                    room.refreshRatingByVisitsAsync().await()
                    if (room.rating.ordinal > rating.ordinal) {
                        rating = room.rating
                    }
                }
            } else {

                val visits = VisitCollection.instance.loadVisitsOfPlace(this@Place, limitLatest10 = false)

                if (visits.isNotEmpty()) {
                    visits.sortByDescending { v -> v.dateTime.timeInMillis }
                    for (v in visits) {
                        if (v.rating == Visit.Rating.Unoccupied || v.rating == Visit.Rating.NotHome) {
                            continue
                        }
                        rating = v.rating
                        break
                    }

                    if (rating == Visit.Rating.Unoccupied) {
                        rating = visits[0].rating
                    }
                }
            }
            Unit
        }
    }

    fun toStringAsync(): Deferred<String> {

        return GlobalScope.async {

            val builder = StringBuilder()

            if (category == Category.Room) {
                val parent = PlaceCollection.instance.loadById(parentId)
                if (parent != null) {
                    if (parent.name.isNotEmpty()) {
                        builder.append(parent.name)
                            .append(" - ")
                    }
                }
            }
            builder.append(name).toString()
        }
    }

}