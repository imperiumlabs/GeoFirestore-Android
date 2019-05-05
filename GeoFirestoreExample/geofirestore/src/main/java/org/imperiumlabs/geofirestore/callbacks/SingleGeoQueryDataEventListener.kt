package org.imperiumlabs.geofirestore.callbacks

import com.google.firebase.firestore.DocumentSnapshot

interface SingleGeoQueryDataEventListener {

    fun onSuccess(documentSnapshots: List<DocumentSnapshot>)

    fun onError(exception: Exception)
}