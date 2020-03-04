package work.ckogyo.returnvisitor.models

import android.content.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.InfoTagCollection
import work.ckogyo.returnvisitor.firebasedb.PersonCollection
import work.ckogyo.returnvisitor.firebasedb.PlaceCollection
import work.ckogyo.returnvisitor.firebasedb.PlacementCollection
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.utils.DataModelKeys.dateTimeMillisKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.personVisitsKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.placeIdKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.ratingKey
import work.ckogyo.returnvisitor.utils.FirebaseCollectionKeys.infoTagIdsKey
import work.ckogyo.returnvisitor.utils.FirebaseCollectionKeys.placementIdsKey
import java.lang.StringBuilder
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
        Unoccupied,
        Negative,
        ForNext,
        NotHome,
        Fair,
        Interested,
        StronglyInterested,
    }

    var rating = Rating.Unoccupied
    val placements = ArrayList<Placement>()
    val infoTags = ArrayList<InfoTag>()

    constructor() : super(idPrefix)

    constructor(lastVisit:Visit): this() {

        place = lastVisit.place
        rating = lastVisit.rating

        for (pv in lastVisit.personVisits) {
            val pv2 = pv.clone()
            personVisits.add(pv2)
        }

        infoTags.clear()
        infoTags.addAll(ArrayList(lastVisit.infoTags))
    }

    constructor(place: Place): this() {
        this.place = place
    }

    fun turnToNotHome(){
        rating = Rating.NotHome
        for (pv in personVisits) {
            pv.seen = false
            pv.isRv = false
            pv.isStudy = false
        }
    }

    /**
     * 日時、レーティングその他基本情報のみを読みだす。
     */
    fun initFromHashMapSimple(map: HashMap<String, Any>) {
        super.initFromHashMap(map)

        var ratingStr = map[ratingKey].toString()
        ratingStr = if (ratingStr == "Indifferent") Rating.ForNext.toString() else ratingStr
        rating = Rating.valueOf(ratingStr)

        dateTime = Calendar.getInstance()
        dateTime.timeInMillis = map[dateTimeMillisKey].toString().toLong()
    }

    suspend fun initVisitFromHashMap(map: HashMap<String, Any>, place2: Place? = null): Visit  = suspendCoroutine { cont ->

        super.initFromHashMap(map)

        var ratingStr = map[ratingKey].toString()
        ratingStr = if (ratingStr == "Indifferent") Rating.ForNext.toString() else ratingStr
        rating = Rating.valueOf(ratingStr)

        dateTime = Calendar.getInstance()
        dateTime.timeInMillis = map[dateTimeMillisKey].toString().toLong()

        GlobalScope.launch {
            val plcIdList = map[placementIdsKey] as? ArrayList<String>
            if (plcIdList != null) {
                placements.clear()
                for (plcId in plcIdList) {
                    val plc = PlacementCollection.instance.loadById(plcId)
                    plc ?: continue
                    placements.add(plc)
                }
            }

            val tagIdList = map[infoTagIdsKey] as? ArrayList<String>
            if (tagIdList != null) {
                infoTags.clear()
                for (tagId in tagIdList) {
                    val tag = InfoTagCollection.instance.loadById(tagId)
                    tag ?: continue
                    infoTags.add(tag)
                }
            }

            val pvMapList = map[personVisitsKey] as? ArrayList<HashMap<String, Any>>

            if (pvMapList != null) {
                personVisits.clear()
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

            val plcIdList = ArrayList<String>()
            for (plc in placements) {
                plcIdList.add(plc.id)
            }
            map[placementIdsKey] = plcIdList

            val tagIdList = ArrayList<String>()
            for (tag in infoTags) {
                tagIdList.add(tag.id)
            }
            map[infoTagIdsKey] = tagIdList

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

        for (plc in placements) {
            cloned.placements.add(plc.clone())
        }

        for (tag in infoTags) {
            cloned.infoTags.add(tag.clone())
        }

        cloned.dateTime = dateTime.clone() as Calendar

        cloned.place = place
        cloned.rating = rating

        return cloned
    }

    fun toString(context: Context, withDateTime: Boolean = true, withLineBreak: Boolean = true):String {

        // TODO: いろいろな情報

        val builder = StringBuilder()
        if (withDateTime) {
            builder.append(dateTime.toDateText(context))
            if (withLineBreak) {
                builder.append(System.lineSeparator())
            } else {
                builder.append(" ")
            }
        }

        if (personVisits.isNotEmpty()) {
            builder.append(toPersonVisitString(context, withLineBreak))

            if (withLineBreak) {
                builder.append(System.lineSeparator())
            } else {
                builder.append(" ")
            }
        }

        if (rvCount > 0 || studyCount > 0 || placementCount > 0 || showVideoCount > 0) {
            if (rvCount > 0) {
                builder.append("${context.getString(R.string.return_visit)}: $rvCount ")
            }

            if (studyCount > 0) {
                builder.append("${context.getString(R.string.study)}: $studyCount ")
            }

            if (placementCount > 0) {
                builder.append("${context.getString(R.string.placement)}: $placementCount ")
            }

            if (showVideoCount > 0) {
                builder.append("${context.getString(R.string.show_video)}: $showVideoCount ")
            }

            if (withLineBreak) {
                builder.append(System.lineSeparator())
            }
        }

        if (infoTags.isNotEmpty()) {
            builder.append("${context.getString(R.string.info_tag)}: ")
            for (i in 0 until infoTags.size) {
                builder.append(infoTags[i].name)

                if (i < infoTags.size - 1) {
                    builder.append(", ")
                }
            }

            if (withLineBreak) {
                builder.append(System.lineSeparator())
            } else {
                builder.append(" ")
            }
        }

        if (placements.isNotEmpty()) {
            builder.append("${context.getString(R.string.placement)}: ")
            for (i in 0 until placements.size) {
                builder.append(placements[i].toShortString(context))

                if (i < placements.size - 1) {
                    builder.append(", ")
                }
            }

            if (withLineBreak) {
                builder.append(System.lineSeparator())
            } else {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    val rvCount: Int
        get() {
            var cnt = 0
            for (pv in personVisits) {
                if (pv.isRv) {
                    cnt++
                }
            }
            return cnt
        }

    private val studyCount: Int
        get() {
            var cnt = 0
            for (pv in personVisits) {
                if (pv.isStudy) {
                    cnt++
                }
            }
            return cnt
        }

    val placementCount: Int
        get() {
            var cnt = 0
            for (plc in placements) {
                if (plc.category != Placement.Category.ShowVideo) {
                    cnt++
                }
            }
            return cnt
        }

    val showVideoCount: Int
        get() {
            var cnt = 0
            for (plc in placements) {
                if (plc.category == Placement.Category.ShowVideo) {
                    cnt++
                }
            }
            return cnt
        }

    fun toPersonVisitString(context: Context, withLineBreak: Boolean = true): String {

        return if (personVisits.isEmpty()) {
            context.getString(R.string.not_seen)
        } else {

            val builder = StringBuilder()

            for (i in 0 until personVisits.size) {
                builder.append(personVisits[i].toString(context, withLineBreak))
                if (i < personVisits.size - 1) {
                    if (withLineBreak) {
                        builder.append(System.lineSeparator())
                    } else {
                        builder.append(", ")
                    }
                }
            }

            builder.toString()
        }
    }

}