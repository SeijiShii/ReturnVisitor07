package work.ckogyo.returnvisitor.firebasedb

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.models.Person
import work.ckogyo.returnvisitor.utils.personsKey
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PersonCollection {

    companion object {

        private val innerInstance = PersonCollection()
        val instance: PersonCollection
            get() = innerInstance
    }

    suspend fun loadById(id: String): Person? = suspendCoroutine { cont ->

        GlobalScope.launch {
            val map = FirebaseDB.instance.loadById(personsKey, id)

            if (map == null) {
                cont.resume(null)
            } else {
                val person = Person()
                person.initFromHashMap(map)
                cont.resume(person)
            }
        }
    }

    fun setAsync(person: Person): Deferred<Boolean> {
        return GlobalScope.async {
            FirebaseDB.instance.set(personsKey, person.id, person.hashMap)
        }
    }

    fun deleteAsync(person: Person): Deferred<Boolean> {
        return GlobalScope.async {
            FirebaseDB.instance.delete(personsKey, person.id)
        }
    }



}