package work.ckogyo.returnvisitor.firebasedb

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import work.ckogyo.returnvisitor.models.Person
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import work.ckogyo.returnvisitor.models.Work
import work.ckogyo.returnvisitor.utils.*
import work.ckogyo.returnvisitor.utils.DataModelKeys.idKey
import work.ckogyo.returnvisitor.utils.FirebaseCollectionKeys.visitsKey
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseDB {

    companion object {
        private val innerInstance = FirebaseDB()
        val instance: FirebaseDB
            get() {
                return  innerInstance
            }

        private lateinit var auth: FirebaseAuth

        fun initialize(fAuth: FirebaseAuth) {
            auth = fAuth
        }
    }
    private val db = FirebaseFirestore.getInstance()

    val userDoc: DocumentReference?
        get(){

            if (auth.currentUser == null) {
                Log.w(debugTag, "No user is logged into Firebase auth!")
                return null
            }

            val uid = auth.currentUser!!.uid
            return db.collection(uid).document(uid)
        }

    // Coroutineの使い方はここが詳しい
    // https://qiita.com/k-kagurazaka@github/items/8595ca60a5c8d31bbe37#%E3%82%B3%E3%83%BC%E3%83%AB%E3%83%90%E3%83%83%E3%82%AF%E3%82%B9%E3%82%BF%E3%82%A4%E3%83%AB%E3%81%8B%E3%82%89%E4%B8%AD%E6%96%AD%E9%96%A2%E6%95%B0%E3%81%B8%E3%81%AE%E5%A4%89%E6%8F%9B


    /**
     * コレクションの属するすべてをHashMapで取得
     */
    suspend fun loadList(colName: String): ArrayList<HashMap<String, Any>> = suspendCoroutine { cont ->

        GlobalScope.launch {
            val list = ArrayList<HashMap<String, Any>>()

            if (userDoc == null) {
                cont.resume(list)
            } else {
                userDoc!!.collection(colName).get().addOnSuccessListener {

                    for (doc in it.documents) {
                        list.add(doc.data as HashMap<String, Any>)
                    }
                    cont.resume(list)

                }.addOnFailureListener {
                    cont.resume(list)
                }
            }
        }
    }

    private suspend fun loadListById(collName: String, id: String): ArrayList<HashMap<String, Any>> = suspendCoroutine {  cont ->
        val list = ArrayList<HashMap<String, Any>>()

        if (userDoc == null) {
            cont.resume(list)
        } else {
            userDoc!!.collection(collName).whereEqualTo(idKey, id).get().addOnSuccessListener {

                for (doc in it.documents) {
                    list.add(doc.data as HashMap<String, Any>)
                }
                cont.resume(list)

            }.addOnFailureListener {
                cont.resume(list)
            }
        }
    }

    suspend fun loadById(collName: String, id: String): (HashMap<String, Any>)? = suspendCoroutine { cont ->

        GlobalScope.launch {
            val mapList = loadListById(collName, id)
            if (mapList.isEmpty()) {
                cont.resume(null)
            } else {
                cont.resume(mapList[0])
            }
        }
    }


    suspend fun set(collName: String,
                    id:String, map:
                    HashMap<String, Any>) = suspendCoroutine<Unit> {

        if (userDoc != null) {
            userDoc!!.collection(collName).document(id).set(map)
        }

        it.resume(Unit)
    }

    suspend fun delete(collName: String,
                       id: String): Boolean = suspendCoroutine { cont ->
        if (userDoc == null) {
            cont.resume(false)
        }

        userDoc!!.collection(collName).document(id).delete().addOnSuccessListener {
            cont.resume(true)
        }.addOnFailureListener {
            cont.resume(false)
        }
    }
}