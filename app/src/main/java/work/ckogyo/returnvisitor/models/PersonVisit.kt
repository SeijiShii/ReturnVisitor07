package work.ckogyo.returnvisitor.models

import android.content.Context
import com.google.firebase.firestore.DocumentReference
import org.json.JSONObject
import work.ckogyo.returnvisitor.utils.*

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

//    constructor(o: JSONObject, context: Context) : super(o){
//
//        seen = o.optBoolean(seenKey)
//        isRv = o.optBoolean(isRVKey)
//        isStudy = o.optBoolean(isStudyKey)
//    }

//    constructor(map: HashMap<String, Any>, userDocument: DocumentReference) : super(map){
//
//
//        val userId = map[userIdKey].toString()
//
//
//        seen = map[seenKey].toString().toBoolean()
//        isRv = map[isRVKey].toString().toBoolean()
//        isStudy = map[isStudyKey].toString().toBoolean()
//    }

    fun initFromHashMap(map: HashMap<String, Any>, userDocument: DocumentReference, onFinish: (pv: PersonVisit) -> Unit) {

        super.initFromHashMap(map)

        seen = map[seenKey].toString().toBoolean()
        isRv = map[isRVKey].toString().toBoolean()
        isStudy = map[isStudyKey].toString().toBoolean()

        val personId = map[personIdKey].toString()
        userDocument.collection(personsKey).document(personId).addSnapshotListener { snapshot, exception ->
            person = Person()
            if (exception == null && snapshot != null) {
                person.initFromHashMap(snapshot.data as HashMap<String, Any>)
            }
            onFinish(this)
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