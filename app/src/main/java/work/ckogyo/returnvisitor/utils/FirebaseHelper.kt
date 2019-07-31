package work.ckogyo.returnvisitor.utils

import com.google.firebase.firestore.DocumentReference
import work.ckogyo.returnvisitor.models.Place
import work.ckogyo.returnvisitor.models.Visit
import kotlin.concurrent.thread

class FirebaseHelper {
    companion object {

        fun loadVisitsOfPlace(userDoc: DocumentReference, place: Place, onFinished: (ArrayList<Visit> ) -> Unit) {

            val visitsToPlace = ArrayList<Visit>()

            userDoc.collection(visitsKey).whereEqualTo(placeIdKey, place.id).addSnapshotListener { snapshot, e ->
                var docCount = 0
                if (e == null && snapshot != null) {
                    docCount += snapshot.documents.size
                    for (doc in snapshot.documents) {
                        val v = Visit()
                        v.fromHashMap(doc.data as HashMap<String, Any>, userDoc, place) {
                            visitsToPlace.add(it)
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
            }
        }
    }
}