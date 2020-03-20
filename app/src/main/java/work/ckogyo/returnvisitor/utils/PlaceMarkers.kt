package work.ckogyo.returnvisitor.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
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

        val marker = googleMap.addMarker(MarkerOptions()
            .position(place.latLng)
            .icon(getIconByResId(context, resId)))
            .also{
                it.isDraggable = true
            }
//            .icon(BitmapDescriptorFactory.fromBitmap(getBitmap(context, resId))))
//            .also {
//            it.isDraggable = true
//        }

        markers.add(marker)
        marker.tag = place.id
        return marker
    }

    private val iconMap = HashMap<Int, BitmapDescriptor>()
    private fun getIconByResId(context: Context, resId: Int): BitmapDescriptor {

        val descriptor = iconMap[resId]

        if (descriptor != null) {
            return descriptor
        }

        val descriptor2 = BitmapDescriptorFactory.fromBitmap(getBitmap(context, resId))
        iconMap[resId] = descriptor2
        return descriptor2
    }

    fun addMarker(context: Context, place: Place): Marker? {
        return addMarker(context, place, ratingToMarkerId(place.rating, place.category))
    }

    fun remove(place: Place) {

        val marker = getMarkerByTag(place.id)
        marker?.remove()
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
        remove(place)
        addMarker(context, place)
    }

    fun clear() {
        for (marker in markers) {
            marker.remove()
        }
        markers.clear()
    }

    fun refreshAllMarkers(context: Context, places: ArrayList<Place>){
        removeUnusedMarkers(places)

        for (place in places) {
            refreshMarker(context, place)
        }
    }

    private fun removeUnusedMarkers(places: ArrayList<Place>) {

        val markersToRemove = ArrayList<Marker>()
        for (marker in markers) {
            var existsPlace = false
            for (place in places) {
                if (place.id == marker.tag) {
                    existsPlace = true
                    break
                }
            }
            if (!existsPlace) {
                markersToRemove.add(marker)
            }
        }

        for (marker in markersToRemove) {
            marker.remove()
            markers.remove(marker)
        }
    }

}