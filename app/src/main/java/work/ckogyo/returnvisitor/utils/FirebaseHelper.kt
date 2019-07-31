package work.ckogyo.returnvisitor.utils

import com.google.firebase.firestore.DocumentReference
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import kotlin.concurrent.thread

class FirebaseHelper {
    companion object {

        fun loadVisitsOfPlace(userDoc: DocumentReference, place: Place, onFinished: (ArrayList<Visit> ) -> Unit) {

            val visitsToPlace = ArrayList<Visit>()

            userDoc.collection(visitsKey).whereEqualTo(placeIdKey, place.id).get().addOnSuccessListener {

                var docCount = 0
                if (it != null) {
                    docCount += it.documents.size
                    for (doc in it.documents) {
                        val v = Visit()
                        v.fromHashMap(doc.data as HashMap<String, Any>, userDoc, place) {it2 ->
                            visitsToPlace.add(it2)
                            docCount--
                        }
                    }
                }

                thread {
                    while (docCount > 0) {
                        Thread.sleep(30)
                    }
                    onFinished(visitsToPlace)
                }
            }.addOnFailureListener {
                onFinished(visitsToPlace)
            }
        }
    }
}