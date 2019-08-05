package work.ckogyo.returnvisitor.models

import java.util.*

class Work : BaseDataModel(idPrefix) {

    companion object {
        const val idPrefix  ="work"
    }

    var start = Calendar.getInstance()
    var end = Calendar.getInstance()

    val duration: Long
    get() = end.timeInMillis - start.timeInMillis

    override fun clone(): BaseDataModel {

        val cloned = Work()
        super.cloneBaseProperties(cloned)

        cloned.start = start.clone() as Calendar
        cloned.end = end.clone() as Calendar

        return cloned
    }
}