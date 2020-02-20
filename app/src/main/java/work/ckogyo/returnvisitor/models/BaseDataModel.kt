package work.ckogyo.returnvisitor.models

import org.json.JSONObject
import work.ckogyo.returnvisitor.utils.DataModelKeys.descriptionKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.idKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.nameKey
import java.util.*
import kotlin.collections.HashMap

abstract class BaseDataModel(){

    var id: String = ""
    var name = ""
    var description = ""

    constructor(idPrefix: String):this(){
        id = generateID(idPrefix)
    }

    constructor(o: JSONObject):this() {
        id = o.optString(idKey)
        name = o.optString(nameKey)
        description = o.optString(descriptionKey)
    }

    open fun initFromHashMap(map: HashMap<String, Any>){
        id = map[idKey].toString()
        name = map[nameKey].toString()
        description = map[descriptionKey].toString()
    }

    private fun generateID(idPrefix: String): String {

        val tick = Calendar.getInstance().timeInMillis.toString().padStart(15, '0')
        val token = (Math.random() * 1000).toInt().toString().padStart(4, '0')

        return "${idPrefix}_$tick$token"
    }

    open val jsonObject:JSONObject
    get() {
        val o = JSONObject()

        o.put(idKey, id)
        o.put(nameKey, name)
        o.put(descriptionKey, description)

        return o
    }

    fun <E:BaseDataModel>cloneBaseProperties(model: E){
        model.id = id
        model.name = name
        model.description = description
    }

    abstract fun clone():BaseDataModel

    override fun equals(other: Any?): Boolean {
        if (other !is BaseDataModel) return false
        return id == other.id
    }

    open val hashMap: HashMap<String, Any>
    get() {
        val map = HashMap<String, Any>()
        map[idKey] = id
        map[nameKey] = name
        map[descriptionKey] = description
        return map
    }
}