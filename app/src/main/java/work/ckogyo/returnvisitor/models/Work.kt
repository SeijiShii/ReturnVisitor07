package work.ckogyo.returnvisitor.models

import work.ckogyo.returnvisitor.utils.endKey
import work.ckogyo.returnvisitor.utils.startKey
import java.util.*
import kotlin.collections.HashMap

class Work : BaseDataModel(idPrefix) {

    companion object {
        const val idPrefix  ="work"
    }

    var start: Calendar = Calendar.getInstance()
    var end: Calendar = Calendar.getInstance()

    val duration: Long
    get() = end.timeInMillis - start.timeInMillis

    override fun clone(): BaseDataModel {

        val cloned = Work()
        super.cloneBaseProperties(cloned)

        cloned.start = start.clone() as Calendar
        cloned.end = end.clone() as Calendar

        return cloned
    }

    override val hashMap: HashMap<String, Any>
        get() {
            val map = super.hashMap

            map[startKey] = start.timeInMillis
            map[endKey] = end.timeInMillis

            return map
        }

    override fun initFromHashMap(map: HashMap<String, Any>) {
        super.initFromHashMap(map)

        start = Calendar.getInstance()
        start.timeInMillis = map[startKey].toString().toLong()

        end = Calendar.getInstance()
        end.timeInMillis = map[endKey].toString().toLong()
    }
}
