package work.ckogyo.returnvisitor.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.map_fragment.*
import work.ckogyo.returnvisitor.MainActivity
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.dialogs.PlaceDialog
import work.ckogyo.returnvisitor.dialogs.PlacePopup
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.utils.*


class MapFragment : Fragment(), OnMapReadyCallback {

    private val mainActivity: MainActivity?
    get() = context as? MainActivity
    lateinit var googleMap: GoogleMap
    private lateinit var placeMarkers: PlaceMarkers

    private val places = ArrayList<Place>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.map_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // https://stackoverflow.com/questions/35496493/getmapasync-in-fragment
        // getMapAsync() in Fragment
        mapView.onCreate(savedInstanceState)

    }

    override fun onMapReady(p0: GoogleMap?) {

        p0?:return
        googleMap = p0
        placeMarkers = PlaceMarkers(googleMap)

        googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID

        mainActivity?.checkPermissionAndEnableMyLocation(googleMap)

        googleMap.setOnMapLongClickListener {

            val place = Place()
            place.latLng = it
            place.category = Place.Category.House

//            RVDB.insertPlace(context!!, place)

            googleMap.animateCamera(CameraUpdateFactory.newLatLng(it))
            val marker = placeMarkers.addMarker(place)

            val popup = PlacePopup(context!!, place)
            popup.onCancel = {
                marker?.remove()
            }

            popup.onClickButton = this::onClickButtonInPlacePopup
            mapOuterFrame.addView(popup)
        }

        googleMap.setOnMarkerClickListener {

            val id = it.tag as? String
            if (id != null) {
                val place = getPlaceById(id)
                if (place != null) {
                    val dialog = PlaceDialog(place)
                    dialog.onClose = this::onClosePlaceDialog
                    dialog.onRefreshPlace = this::onRefreshPlaceInPlaceDialog
                    mainActivity?.showDialog(dialog)
                }
            }
            return@setOnMarkerClickListener true
        }

        loadCameraPosition()
        loadPlaces{
            showPlacePins()
        }
    }

    private fun onRefreshPlaceInPlaceDialog(place: Place) {

    }

    private fun loadPlaces(onLoaded: () -> Unit) {

        mainActivity?:return

        places.clear()
        mainActivity!!.dbRef.userDocument.collection(placesKey).get()
            .addOnSuccessListener {
                for (doc in it) {
                    val map = doc.data
                    map?: continue
                    val place = Place()
                    place.initFromHashMap(map as HashMap<String, Any>)
                    places.add(place)
                }
                onLoaded()
            }
            .addOnFailureListener {

            }

    }

    private fun onClosePlaceDialog(place: Place, param: OnFinishEditParam) {

        when(param) {
            OnFinishEditParam.Deleted -> {
                places.remove(place)
                placeMarkers.remove(place)
            }
        }

    }

    private fun onClickButtonInPlacePopup(place: Place) {
        mainActivity?.showRecordVisitFragmentForNewPlace(place, this::onFinishEditVisit)
    }

    override fun onResume() {
        super.onResume()

        mapView.onResume()

        mapView.getMapAsync(this)
    }

    override fun onPause() {
        super.onPause()

        saveCameraPosition()
    }

    private fun saveCameraPosition() {
        mainActivity?:return

        val pos = googleMap.cameraPosition?:return

        val editor = mainActivity!!.getSharedPreferences(returnVisitorPrefsKey, Context.MODE_PRIVATE).edit()
        editor.putFloat(zoomLevelKey, pos.zoom)
        editor.putString(latitudeKey, pos.target.latitude.toString())
        editor.putString(longitudeKey, pos.target.longitude.toString())
        editor.apply()
    }

    private fun loadCameraPosition() {

        mainActivity?:return

        val prefs = mainActivity!!.getSharedPreferences(returnVisitorPrefsKey, Context.MODE_PRIVATE)

        val lat = (prefs.getString(latitudeKey, null)?.toDouble()?:0.0)
        val lng = (prefs.getString(longitudeKey, null)?.toDouble()?:0.0)

        val latLng = LatLng(lat, lng)
        val zoom = prefs.getFloat(zoomLevelKey, 3f)

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
    }

    private fun showPlacePins() {

        mainActivity?:return

        for (place in places) {
            placeMarkers.addMarker(place)
        }
    }

    private fun onFinishEditVisit(visit: Visit, param: OnFinishEditParam) {

        placeMarkers.remove(visit.place)

        when(param) {
            OnFinishEditParam.Canceled -> {}
            OnFinishEditParam.Done -> {
                mainActivity?:return

                places.add(visit.place)

                val handler = Handler()

                val visitsToPlace = ArrayList<Visit>()

                FirebaseHelper.loadVisitsOfPlace(mainActivity!!.dbRef.userDocument, visit.place){
                    visitsToPlace.addAll(it)
                    visitsToPlace.add(visit)
                    visit.place.refreshRatingByVisits(visitsToPlace)
                    mainActivity!!.dbRef.userDocument.collection(placesKey)
                        .document(visit.place.id).set(visit.place.hashMap)
                    handler.post {
                        placeMarkers.addMarker(visit.place)
                    }
                }

                mainActivity!!.dbRef.userDocument.collection(visitsKey).document(visit.id).set(visit.hashMap)

                for (person in visit.persons) {
                    mainActivity!!.dbRef.userDocument.collection(personsKey).document(person.id).set(person.hashMap)
                }
            }
            OnFinishEditParam.Deleted -> {

            }
        }
    }

    private fun getPlaceById(id: String):Place? {
        for (place in places) {
            if (place.id == id) {
                return place
            }
        }
        return null
    }


}