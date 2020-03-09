package work.ckogyo.returnvisitor.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit

class PlaceMarkers(private val googleMap: GoogleMap) {

    companion object {
        private fun ratingToMarkerId(rating: Visit.Rating, category: Place.Category):Int {
            return when(category) {
                Place.Category.Place -> roundMarkerIds[rating.ordinal]
                Place.Category.House -> pinMarkerIds[rating.ordinal]
                Place.Category.HousingComplex -> squareMarkerIds[rating.ordinal]
                Place.Category.Room -> roundMarkerIds[rating.ordinal]
            }
        }
    }

    private val markers = ArrayList<Marker>()

    private fun addMarker(context: Context, place: Place, resId:Int):Marker? {

//        Log.d(debugTag, "addMarker place.rating: ${place.id} ${place.category} ${place.rating}")

        val marker = googleMap.addMarker(MarkerOptions()
            .position(place.latLng)
            .icon(BitmapDescriptorFactory.fromBitmap(getBitmap(context, resId)))).also {
            it.isDraggable = true
        }

        markers.add(marker)
        marker.tag = place.id
        return marker
    }

    fun addMarker(context: Context, place: Place): Marker? {
        return addMarker(context, place, ratingToMarkerId(place.rating, place.category))
    }

    fun remove(place: Place) {

//        Log.d(debugTag, "remove: ${place.id} ${place.category} ${place.rating}")
        val marker = getMarkerByTag(place.id)
//        Log.d(debugTag, "getMarkerByTag remove: ${place.id} ${place.category} ${place.rating}")
        marker?.remove()
//        Log.d(debugTag, "after remove: ${place.id} ${place.category} ${place.rating}")
    }

    private fun getMarkerByTag(tag: String):Marker? {

        for (marker in markers) {
            if ((marker.tag as? String) == tag) {
                return marker
            }
        }
        return null
    }

    fun refreshMarker(context: Context, place: Place) {
//        Log.d(debugTag, "refreshMarker: ${place.id} ${place.category} ${place.rating}")
        remove(place)
        addMarker(context, place)
    }

    fun clear() {
        for (marker in markers) {
            marker.remove()
        }
        markers.clear()
    }


}