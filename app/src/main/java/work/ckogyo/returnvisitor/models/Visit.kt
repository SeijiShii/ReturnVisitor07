package work.ckogyo.returnvisitor.models

import android.content.Context
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.*
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.utils.DataModelKeys.dateTimeMillisKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.infoTagModelsKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.personVisitsKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.placeModelKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.placementModelsKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.ratingKey
import work.ckogyo.returnvisitor.utils.FirebaseCollectionKeys.placementsKey
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
        initSimpleDataFromHashMap(map)
    }

    override fun initFromHashMap(map: HashMap<String, Any>) {
        super.initFromHashMap(map)

        initSimpleDataFromHashMap(map)

        placements.clear()
        val plcMapList = map[placementModelsKey] as ArrayList<HashMap<String, Any>>
        for (plcMap in plcMapList) {
            val plc = Placement().apply {
                initFromHashMap(plcMap)
            }
            placements.add(plc)
        }

        infoTags.clear()
        val infoTagMapList = map[infoTagModelsKey] as ArrayList<HashMap<String, Any>>
        for (infoTagMap in infoTagMapList) {
            val infoTag = InfoTag().apply {
                initFromHashMap(infoTagMap)
            }
            infoTags.add(infoTag)
        }

        val pvMapList = map[personVisitsKey] as? ArrayList<HashMap<String, Any>>
        if (pvMapList != null) {
            personVisits.clear()
            for (pvMap in pvMapList) {
                val pv = PersonVisit().apply {
                    initFromHashMap(pvMap)
                }
                personVisits.add(pv)
            }
        }

        place = Place()
        place.initFromHashMap(map[placeModelKey] as HashMap<String, Any>)

    }

    private fun initSimpleDataFromHashMap(map: HashMap<String, Any>) {
        var ratingStr = map[ratingKey].toString()
        ratingStr = if (ratingStr == "Indifferent") Rating.ForNext.toString() else ratingStr
        rating = Rating.valueOf(ratingStr)

        dateTime = Calendar.getInstance()
        dateTime.timeInMillis = map[dateTimeMillisKey].toString().toLong()
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

            map[placeModelKey] = place.hashMap
//            map[placeIdKey] = place.id


            val plcMapList = ArrayList<HashMap<String, Any>>()
            for (plc in placements) {
                plcMapList.add(plc.hashMap)
            }
            map[placementModelsKey] = plcMapList

            val infoTagMapList = ArrayList<HashMap<String, Any>>()
            for (tag in infoTags) {
                infoTagMapList.add(tag.hashMap)
            }
            map[infoTagModelsKey] = infoTagMapList

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

        if (builder.isEmpty()) {
            builder.append(place.toString())
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

    /**
     * そのVisitで誰かに会っているか
     */
    val isSeen: Boolean
        get() {
            for (pv in personVisits) {
                if (pv.seen) {
                    return true
                }
            }
            return false
        }

    fun hasPerson(person: Person):Boolean {
        for (pv in personVisits) {
            if (pv.person == person) {
                return true
            }
        }
        return false
    }

    fun replacePersonIfHas(person: Person) {
        for (pv in personVisits) {
            if (pv.person == person) {
                pv.person = person
            }
        }
    }

}