package work.ckogyo.returnvisitor.firebasedb

import kotlinx.coroutines.GlobalScope
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

    suspend fun set(person: Person): Boolean = suspendCoroutine { cont ->
        GlobalScope.launch {
            cont.resume(FirebaseDB.instance.set(personsKey, person.id, person.hashMap))
        }
    }

    suspend fun delete(person: Person): Boolean = suspendCoroutine { cont ->
        GlobalScope.launch {
            cont.resume(FirebaseDB.instance.delete(personsKey, person.id))
        }
    }



}