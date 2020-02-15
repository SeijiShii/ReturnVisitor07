package work.ckogyo.returnvisitor.models

import work.ckogyo.returnvisitor.utils.toDurationText
import java.util.*
import kotlin.collections.ArrayList

class DailyReport(val day: Calendar) {
    var isDummy = false
    lateinit var works: ArrayList<Work>
    lateinit var visits: ArrayList<Visit>

    private val duration: Long
        get() {
            var sum = 0L
            for (work in works) {
                sum += work.duration
            }
            return sum
        }

    val durationString: String
        get() {
            val dur = duration
            return if (dur <= 0) "" else dur.toDurationText()
        }
}