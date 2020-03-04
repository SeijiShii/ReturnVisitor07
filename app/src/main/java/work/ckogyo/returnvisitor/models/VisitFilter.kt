package work.ckogyo.returnvisitor.models

import android.content.Context
import org.json.JSONObject
import work.ckogyo.returnvisitor.utils.SharedPrefKeys.filterNumberKey
import work.ckogyo.returnvisitor.utils.SharedPrefKeys.filterPeriodEndKey
import work.ckogyo.returnvisitor.utils.SharedPrefKeys.filterPeriodKey
import work.ckogyo.returnvisitor.utils.SharedPrefKeys.filterPeriodStartKey
import work.ckogyo.returnvisitor.utils.SharedPrefKeys.filterRatingsKey
import work.ckogyo.returnvisitor.utils.SharedPrefKeys.filterUnitKey
import work.ckogyo.returnvisitor.utils.SharedPrefKeys.returnVisitorPrefsKey
import work.ckogyo.returnvisitor.utils.cloneWith0Time
import work.ckogyo.returnvisitor.utils.isDateBefore
import java.util.*
import kotlin.collections.HashSet

class VisitFilter {

    enum class PeriodTerm{
        OneYear,
        SixMonths,
        ThreeMonths,
        OneMonth,
        OneWeek,
        Yesterday,
        Today
    }

    companion object {
        fun loadFromSharedPreferences(context: Context): VisitFilter {

            val filter = VisitFilter().also {
                it.ratings.clear()
            }

            val prefs = context.getSharedPreferences(returnVisitorPrefsKey, Context.MODE_PRIVATE)

            val set = prefs.getStringSet(filterRatingsKey, setOf())
            for (str in set!!) {
                filter.ratings.add(Visit.Rating.valueOf(str))
            }

            val startStr = prefs.getString(filterPeriodStartKey, null)
            if (startStr != null) {
                filter.periodStart = PeriodTerm.valueOf(startStr)
            }

            val endStr = prefs.getString(filterPeriodEndKey, null)
            if (endStr != null) {
                filter.periodEnd = PeriodTerm.valueOf(endStr)
            }

            return filter
        }
    }

    var periodStart = PeriodTerm.OneMonth
    var periodEnd = PeriodTerm.OneWeek

    private fun periodTermToCalendar(term: PeriodTerm): Calendar {

        val cal = Calendar.getInstance()

        when(term) {
            PeriodTerm.OneYear -> {
                cal.add(Calendar.YEAR, -1)
            }
            PeriodTerm.SixMonths -> {
                cal.add(Calendar.MONTH, -6)
            }
            PeriodTerm.ThreeMonths -> {
                cal.add(Calendar.MONTH, -3)
            }
            PeriodTerm.OneMonth -> {
                cal.add(Calendar.MONTH, -1)
            }
            PeriodTerm.OneWeek -> {
                cal.add(Calendar.WEEK_OF_MONTH, -1)
            }
            PeriodTerm.Yesterday -> {
                cal.add(Calendar.DAY_OF_MONTH, -1)
            }
            else -> {}
        }
        return cal
    }

    val periodStartDate: Calendar
         get() {
             val d = periodTermToCalendar(periodStart)
             return d.cloneWith0Time()
         }

    val periodEndDate: Calendar
        get() {
            var d = periodTermToCalendar(periodEnd)
            d.add(Calendar.DAY_OF_MONTH, 1)
            d = d.cloneWith0Time()
            d.timeInMillis -= 1
            return d
        }

//    var period = Period().also {
//        it.start = PeriodPair(PeriodUnit.Month, 1)
//        it.end = PeriodPair(PeriodUnit.Day, 1)
//    }

    var ratings = arrayListOf(Visit.Rating.NotHome,
        Visit.Rating.Fair,
        Visit.Rating.Interested,
        Visit.Rating.StronglyInterested)

    fun save(context: Context) {

        context.getSharedPreferences(returnVisitorPrefsKey, Context.MODE_PRIVATE)
            .edit()
            .putString(filterPeriodStartKey, periodStart.toString())
            .putString(filterPeriodEndKey, periodEnd.toString())
            .putStringSet(filterRatingsKey, ratingSet)
            .apply()

    }

    private val ratingSet: HashSet<String>
        get() {
            val set = HashSet<String>()
            for (r in ratings) {
                set.add(r.toString())
            }
            return set
        }
}

//class PeriodPair(var unit: VisitFilter.PeriodUnit, var number: Int) {
//
//    companion object {
//        fun fromJson(json: String): PeriodPair {
//            val o = JSONObject(json)
//            val unit = VisitFilter.PeriodUnit.valueOf(o[filterUnitKey].toString())
//            val number = o[filterNumberKey].toString().toInt()
//            return PeriodPair(unit, number)
//        }
//    }
//
//    val jsonString: String
//        get() {
//            val o = JSONObject()
//            o.put(filterUnitKey, unit.toString())
//            o.put(filterNumberKey, number)
//            return o.toString()
//        }
//
//    val date: Calendar
//        get() {
//            val d = Calendar.getInstance()
//            val calenderUnit = when(unit) {
//                VisitFilter.PeriodUnit.Day -> Calendar.DAY_OF_MONTH
//                VisitFilter.PeriodUnit.Week -> Calendar.WEEK_OF_MONTH
//                VisitFilter.PeriodUnit.Month -> Calendar.MONTH
//                VisitFilter.PeriodUnit.Year -> Calendar.YEAR
//            }
//            d.add(calenderUnit, number)
//            return d
//        }
//
//    fun isBeforeOther(other: PeriodPair): Boolean {
//        return date.isDateBefore(other.date, allowSame = true)
//    }
//
//    fun clone(): PeriodPair {
//        return PeriodPair(unit, number)
//    }
//}
//
//class Period {
//
//    var start: PeriodPair? = null
//        set(value) {
//            if (value == null) {
//                field = value
//            } else {
//                if (end == null) {
//                    field = value
//                } else {
//                    if (!value.isBeforeOther(end!!))    {
//                        field = end!!.clone()
//                    }
//                }
//            }
//        }
//
//    var end: PeriodPair? = null
//        set(value) {
//            if (value == null) {
//                field = value
//            } else {
//                if (start == null) {
//                    field = value
//                } else {
//                    if (!start!!.isBeforeOther(value))    {
//                        field = start!!.clone()
//                    }
//                }
//            }
//        }
//
//    companion object {
//        fun fromJson(json: String): Period {
//
//            val o = JSONObject(json)
//            val period = Period()
//
//            val startJson = o[filterPeriodStartKey].toString()
//            if (startJson.isNotEmpty()) {
//                period.start = PeriodPair.fromJson(startJson)
//            }
//
//            val endJson = o[filterPeriodEndKey].toString()
//            if (endJson.isNotEmpty()) {
//                period.end = PeriodPair.fromJson(endJson)
//            }
//
//            return period
//        }
//    }
//
//    val jsonString: String
//        get() {
//            val o = JSONObject()
//            o.put(filterPeriodStartKey, start?.jsonString ?: "")
//            o.put(filterPeriodEndKey, end?.jsonString ?: "")
//            return o.toString()
//        }
//
//}