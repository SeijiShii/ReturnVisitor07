package work.ckogyo.returnvisitor.models

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.DocumentReference
import work.ckogyo.returnvisitor.utils.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread

class Visit : BaseDataModel {

    val personVisits = ArrayList<PersonVisit>()
    var dateTime: Calendar = Calendar.getInstance()
    var place = Place()

    enum class Rating {
        None,
        Negative,
        Indifferent,
        NotHome,
        Fair,
        Interested,
        StronglyInterested,
    }

    var rating = Rating.None

    constructor() : super(idPrefix)

    fun fromHashMap(map: HashMap<String, Any>, userDoc: DocumentReference, place2: Place? = null, onFinish: (Visit) -> Unit) {

        super.initFromHashMap(map)

        rating = Rating.valueOf(map[ratingKey].toString())

        dateTime = Calendar.getInstance()
        dateTime.timeInMillis = map[dateTimeMllisKey].toString().toLong()

        val pvMapList = map[personVisitsKey] as ArrayList<HashMap<String, Any>>
        var pvCount = pvMapList.size
        for (pvm in pvMapList) {
            val pv = PersonVisit()
            pv.initFromHashMap(pvm, userDoc) {
                pvCount--
            }
        }

        val placeId = map[placesKey].toString()

        var placeLoaded = false
        if (place2 != null) {
            place = place2
            placeLoaded = true
        } else {
            userDoc.collection(placesKey).document(placeId).addSnapshotListener { snapshot, ex ->
                if (ex == null) {
                    if (snapshot != null) {
                        place = Place()
                        val map2 = snapshot.data as? HashMap<String, Any>
                        if (map2 != null) {
                            place.initFromHashMap(map2)
                        }
                    }
                } else {
                    Log.d(debugTag, ex.message)
                }
                placeLoaded = true
            }
        }


        thread {
            while (pvCount > 0 || !placeLoaded) {
                Thread.sleep(30)
            }
            onFinish(this)
        }
    }

    override val hashMap: HashMap<String, Any>
        get() {
            val map = super.hashMap

            map[ratingKey] = rating
            map[dateTimeMllisKey] = dateTime.timeInMillis

            val personVisitList = ArrayList<HashMap<String, Any>>()
            for (pv in personVisits) {
                personVisitList.add(pv.hashMap)
            }
            map[personVisitsKey] = personVisitList
            map[placeIdKey] = place.id

            return map
        }

    val persons: ArrayList<Person>
    get() {
        val persons = ArrayList<Person>()
        for (pv in personVisits) {
            persons.add(pv.person)
        }
        return persons
    }

    companion object {
        const val idPrefix = "visit"
    }

    override fun clone():Visit {

        val cloned = Visit()

        super.cloneBaseProperties(cloned)

        for(pv in personVisits) {
            cloned.personVisits.add(pv.clone())
        }

        return cloned
    }

    fun toString(context: Context):String {

        // TODO: いろいろな情報

        return toDateTimeString(context)
    }

    fun toDateTimeString(context: Context):String {
        val dateFormat = android.text.format.DateFormat.getDateFormat(context)
        val timeFormat = android.text.format.DateFormat.getTimeFormat(context)

        return "${dateFormat.format(dateTime.time)} ${timeFormat.format(dateTime.time)}"
    }

}