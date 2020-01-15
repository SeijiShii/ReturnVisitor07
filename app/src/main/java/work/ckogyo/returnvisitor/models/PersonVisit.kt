package work.ckogyo.returnvisitor.models

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import work.ckogyo.returnvisitor.firebasedb.PersonCollection
import work.ckogyo.returnvisitor.utils.*
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

    suspend fun initFromHashMap(map: HashMap<String, Any>, personColl: PersonCollection): PersonVisit = suspendCoroutine { cont ->

        super.initFromHashMap(map)

        seen = map[seenKey].toString().toBoolean()
        isRv = map[isRVKey].toString().toBoolean()
        isStudy = map[isStudyKey].toString().toBoolean()

        val personId = map[personIdKey].toString()

        GlobalScope.launch {
            val person2 = personColl.loadById(personId)
            if (person2 != null) {
                person = person2
            }
            cont.resume(this@PersonVisit)
        }
    }

    override val jsonObject: JSONObject
        get() {
            val o = super.jsonObject

            o.put(personIdKey, person.id)
            o.put(seenKey, seen)
            o.put(isRVKey, isRv)
            o.put(isStudyKey, isStudy)

            return o
        }

    override val hashMap: HashMap<String, Any>
        get() {
            val map = super.hashMap

            map[personIdKey] = person.id
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
}