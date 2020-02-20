package work.ckogyo.returnvisitor.firebasedb

import com.google.firebase.firestore.Query
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.models.Placement
import work.ckogyo.returnvisitor.utils.DataModelKeys.lastUsedAtInMillisKey
import work.ckogyo.returnvisitor.utils.FirebaseCollectionKeys.placementsKey
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PlacementCollection {
    companion object {

        private val innerInstance = PlacementCollection()
        val instance: PlacementCollection
            get() = innerInstance
    }

    suspend fun loadById(id: String): Placement? = suspendCoroutine {
        GlobalScope.launch {
            val map = FirebaseDB.instance.loadById(placementsKey, id)
            if (map == null) {
                it.resume(null)
            } else {
                val plc = Placement()
                plc.initFromHashMap(map)
                it.resume(plc)
            }
        }
    }

    fun setAsync(plc: Placement): Deferred<Unit> {
        return GlobalScope.async {
            FirebaseDB.instance.set(placementsKey, plc.id, plc.hashMap)
        }
    }

    fun deleteAsync(plc: Placement): Deferred<Boolean> {
        return GlobalScope.async {
            FirebaseDB.instance.delete(placementsKey, plc.id)
        }
    }

    suspend fun loadInLatestUseOrder(): ArrayList<Placement> = suspendCoroutine {  cont ->

        val plcs = ArrayList<Placement>()
        val userDoc = FirebaseDB.instance.userDoc
        if (userDoc != null) {
            GlobalScope.launch {
                userDoc.collection(placementsKey)
                    .orderBy(lastUsedAtInMillisKey, Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener {
                        for (doc in it.documents) {
                            val data = doc.data as HashMap<String, Any>
                            val plc = Placement()
                            plc.initFromHashMap(data)
                            plcs.add(plc)
                        }
                        cont.resume(plcs)
                    }
                    .addOnFailureListener {
                        cont.resume(plcs)
                    }
            }
        }


    }
}