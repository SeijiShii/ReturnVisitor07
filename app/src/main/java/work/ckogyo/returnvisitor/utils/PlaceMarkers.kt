package work.ckogyo.returnvisitor.utils

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit

class PlaceMarkers(private val googleMap: GoogleMap) {

    companion object {
        val markerIds = arrayOf(
            R.mipmap.gray_pin,
            R.mipmap.red_pin,
            R.mipmap.purple_pin,
            R.mipmap.blue_pin,
            R.mipmap.green_pin,
            R.mipmap.yellow_pin,
            R.mipmap.orange_pin
        )

        private fun ratingToMarkerId(rating: Visit.Rating):Int {
            return markerIds[rating.ordinal]
        }
    }

    private val markers = ArrayList<Marker>()

    private fun addMarker(place: Place, resId:Int):Marker? {
        val marker = googleMap.addMarker(MarkerOptions().position(place.latLng).icon(BitmapDescriptorFactory.fromResource(resId)))
        markers.add(marker)
        marker.tag = place.id
        return marker
    }

    fun addMarker(place: Place): Marker? {
        return addMarker(place, ratingToMarkerId(place.rating))
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



}