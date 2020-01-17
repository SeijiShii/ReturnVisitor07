package work.ckogyo.returnvisitor.models

import android.content.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import work.ckogyo.returnvisitor.firebasedb.FirebaseDB
import work.ckogyo.returnvisitor.firebasedb.PersonCollection
import work.ckogyo.returnvisitor.firebasedb.PlaceCollection
import work.ckogyo.returnvisitor.utils.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

    constructor(lastVisit:Visit): this() {

        place = lastVisit.place
        rating = lastVisit.rating

        for (pv in lastVisit.personVisits) {
            val pv2 = pv.clone()
            personVisits.add(pv2)
        }
    }

    fun turnToNotHome(){
        rating = Rating.NotHome
        for (pv in personVisits) {
            pv.seen = false
            pv.isRv = false
            pv.isStudy = false
        }
    }

    suspend fun initVisitFromHashMap(map: HashMap<String, Any>, place2: Place? = null): Visit  = suspendCoroutine { cont ->

        super.initFromHashMap(map)

        rating = Rating.valueOf(map[ratingKey].toString())

        dateTime = Calendar.getInstance()
        dateTime.timeInMillis = map[dateTimeMillisKey].toString().toLong()

        val pvMapList = map[personVisitsKey] as ArrayList<HashMap<String, Any>>
        personVisits.clear()

        GlobalScope.launch {
            for (pvm in pvMapList) {
                val pv = PersonVisit().initFromHashMap(pvm, PersonCollection.instance)
                personVisits.add(pv)
            }

            val placeId = map[placeIdKey].toString()

            if (place2 != null) {
                place = place2
            } else {
                val place3 = PlaceCollection.instance.loadById(placeId)
                if (place3 != null) {
                    place = place3
                }
            }
            cont.resume(this@Visit)
        }
    }

    override val hashMap: HashMap<String, Any>
        get() {
            val map = super.hashMap

            map[ratingKey] = rating
            map[dateTimeMillisKey] = dateTime.timeInMillis

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

        cloned.dateTime = dateTime.clone() as Calendar

        cloned.place = place
        cloned.rating = rating

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