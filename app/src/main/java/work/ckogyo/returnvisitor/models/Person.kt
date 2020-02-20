package work.ckogyo.returnvisitor.models

import android.content.Context
import org.json.JSONObject
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.utils.DataModelKeys.ageKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.sexKey

open class Person : BaseDataModel {

    enum class Sex{
        Unknown,
        Male,
        Female
    }

    enum class Age {
        Unknown,
        To10,
        From11To20,
        From21To30,
        From31To40,
        From41To50,
        From51To60,
        From61To70,
        From71To80,
        From81,
    }

    companion object {
        const val idPrefix = "person"
    }

    constructor():super(idPrefix)

    constructor(o:JSONObject):super(o) {

        if (o.has(sexKey)) {
            sex = Sex.valueOf(o.getString(sexKey))
        }

        if (o.has(ageKey)) {
            age = Age.valueOf(o.getString(ageKey))
        }
    }

    override fun initFromHashMap(map: HashMap<String, Any>) {
        super.initFromHashMap(map)

        sex = Sex.valueOf(map[sexKey].toString())
        age = Age.valueOf(map[ageKey].toString())
    }

    var sex = Sex.Unknown
    var age = Age.Unknown

    override val jsonObject: JSONObject
        get(){
            val o = super.jsonObject

            o.put(sexKey, sex.toString())
            o.put(ageKey, age.toString())

            return o
        }

    override val hashMap: HashMap<String, Any>
        get() {
            val map = super.hashMap

            map[sexKey] = sex
            map[ageKey] = age

            return map
        }

    override fun clone():Person {

        val cloned = Person()

        super.cloneBaseProperties(cloned)

        cloned.sex = sex
        cloned.age = age

        return cloned
    }

    fun toString(context: Context):String {

        return if (name.isNotEmpty()) {
            name
        } else {
            val sexStr = when(sex) {
                Sex.Unknown -> context.getString(R.string.unknown)
                Sex.Male -> context.getString(R.string.male)
                Sex.Female -> context.getString(R.string.female)
            }

            val ageArray = context.resources.getStringArray(R.array.ageArray)

            "$sexStr ${ageArray[age.ordinal]}"
        }
    }
}