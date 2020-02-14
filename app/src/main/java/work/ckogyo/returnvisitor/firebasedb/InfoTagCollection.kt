package work.ckogyo.returnvisitor.firebasedb

import com.google.firebase.firestore.Query
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.models.InfoTag
import work.ckogyo.returnvisitor.utils.infoTagsKey
import work.ckogyo.returnvisitor.utils.lastUsedAtInMillisKey
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class InfoTagCollection {

    companion object {

        private val innerInstance = InfoTagCollection()
        val instance: InfoTagCollection
            get() = innerInstance
    }

    suspend fun loadById(id: String): InfoTag? = suspendCoroutine {
        GlobalScope.launch {
            val map = FirebaseDB.instance.loadById(infoTagsKey, id)
            if (map == null) {
                it.resume(null)
            } else {
                val infoTag = InfoTag()
                infoTag.initFromHashMap(map)
                it.resume(infoTag)
            }
        }
    }

    fun setAsync(tag: InfoTag): Deferred<Unit> {
        return GlobalScope.async {
            FirebaseDB.instance.set(infoTagsKey, tag.id, tag.hashMap)
        }
    }

    fun deleteAsync(tag: InfoTag): Deferred<Boolean> {
        return GlobalScope.async {
            FirebaseDB.instance.delete(infoTagsKey, tag.id)
        }
    }

    suspend fun loadInLatestUseOrder(): ArrayList<InfoTag> = suspendCoroutine {  cont ->

        val tags = ArrayList<InfoTag>()
        val userDoc = FirebaseDB.instance.userDoc
        if (userDoc != null) {
            GlobalScope.launch {
                userDoc.collection(infoTagsKey)
                    .orderBy(lastUsedAtInMillisKey, Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener {
                        for (doc in it.documents) {
                            val data = doc.data as HashMap<String, Any>
                            val tag = InfoTag()
                            tag.initFromHashMap(data)
                            tags.add(tag)
                        }
                        cont.resume(tags)
                    }
                    .addOnFailureListener {
                        cont.resume(tags)
                    }
            }
        }


    }
}