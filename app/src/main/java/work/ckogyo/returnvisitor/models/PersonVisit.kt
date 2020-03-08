package work.ckogyo.returnvisitor.models

import android.content.Context
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONObject
import work.ckogyo.returnvisitor.R
import work.ckogyo.returnvisitor.firebasedb.FirebaseDB
import work.ckogyo.returnvisitor.utils.DataModelKeys.isRVKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.isStudyKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.personModelKey
import work.ckogyo.returnvisitor.utils.DataModelKeys.seenKey
import java.lang.StringBuilder
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PersonVisit : BaseDataModel {

    companion object {
        const val idPrefix = "person_visit"
    }

    var person: Person = Person()
    var seen = false
    var isRv = false
    var isStudy = false

    constructor() : super(idPrefix)

    constructor(person: Person):this(){
        this.person = person
    }

    override fun initFromHashMap(map: HashMap<String, Any>) {
        super.initFromHashMap(map)

        seen = map[seenKey].toString().toBoolean()
        isRv = map[isRVKey].toString().toBoolean()
        isStudy = map[isStudyKey].toString().toBoolean()

        person = Person()
        person.initFromHashMap(map[personModelKey] as HashMap<String, Any>)
    }

//    override fun initFromHashMap(map: HashMap<String, Any>): PersonVisit  {
//
//        super.initFromHashMap(map)
//
//        seen = map[seenKey].toString().toBoolean()
//        isRv = map[isRVKey].toString().toBoolean()
//        isStudy = map[isStudyKey].toString().toBoolean()
//
//        person = Person()
//        person.initFromHashMap(map[personModelKey] as HashMap<String, Any>)
//
//        return this
//    }

//    fun initFromHashMapAsync(map: HashMap<String, Any>): Deferred<PersonVisit> {
//        return GlobalScope.async {
//            initFromHashMapSuspend(map)
//        }
//    }

    override val jsonObject: JSONObject
        get() {
            val o = super.jsonObject

            o.put(personModelKey, person.hashMap)
            o.put(seenKey, seen)
            o.put(isRVKey, isRv)
            o.put(isStudyKey, isStudy)

            return o
        }

    override val hashMap: HashMap<String, Any>
        get() {
            val map = super.hashMap

            map[personModelKey] = person.hashMap
//            map[personIdKey] = person.id
            map[seenKey] = seen
            map[isRVKey] = isRv
            map[isStudyKey] = isStudy

            return map
        }

    override fun clone(): PersonVisit {

        val cloned = PersonVisit()

        cloned.person = person.clone()
        cloned.seen = seen
        cloned.isRv = isRv
        cloned.isStudy = isStudy

        return cloned
    }

    fun toString(context: Context, withLineBreak: Boolean = true): String {

        return StringBuilder().also {
            it.append(person.toString(context))
            if (withLineBreak) {
                it.append(System.lineSeparator())
            } else {
                it.append(" ")
            }

            it.append(context.getText(if (seen) R.string.seen else R.string.not_seen))
            if (isRv) it.append(context.getText(R.string.return_visit))
            if (isStudy) it.append(context.getText(R.string.study))
        }.toString()
    }

}