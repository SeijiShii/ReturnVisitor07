package work.ckogyo.returnvisitor.models

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.*
import org.json.JSONObject
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.FirebaseDB
import work.ckogyo.returnvisitor.firebasedb.PlaceCollection
import work.ckogyo.returnvisitor.firebasedb.VisitCollection
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.utils.DataModelKeys.addressKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.categoryKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.parentIdKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.parentNameKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.ratingKey
import work.ckogyo.returnvisitor.utils.SharedPrefKeys.latitudeKey
import work.ckogyo.returnvisitor.utils.SharedPrefKeys.longitudeKey
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

        var ratingStr = map[ratingKey].toString()
        ratingStr = if (ratingStr == "Indifferent") Visit.Rating.ForNext.toString() else ratingStr
        rating = Visit.Rating.valueOf(ratingStr)

        if (category == Category.Room) {
            parentId = map[parentIdKey].toString()
            parentName = map[parentNameKey]?.toString() ?: ""
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
    var parentName = ""

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
                map[parentNameKey] = parentName
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

    override fun toString(): String {

        val builder = StringBuilder()

        if (address.isNotEmpty()) {
            builder.append(address).append(" ")
        }

        if (category == Category.Room && parentName.isNotEmpty()) {
            builder.append(parentName)
                .append(" - ")
        }

        if (name.isNotEmpty()) {
            builder.append(name)
        }

        return builder.toString()
    }

//    fun toStringAsync(): Deferred<String> {
//
//        return GlobalScope.async {
//
//            val builder = StringBuilder()
//
//            if (category == Category.Room) {
//                val parent = FirebaseDB.instance.loadPlaceById(parentId)
//                if (parent != null) {
//                    if (parent.name.isNotEmpty()) {
//                        builder.append(parent.name)
//                            .append(" - ")
//                    }
//                }
//            }
//            builder.append(name).toString()
//        }
//    }

}