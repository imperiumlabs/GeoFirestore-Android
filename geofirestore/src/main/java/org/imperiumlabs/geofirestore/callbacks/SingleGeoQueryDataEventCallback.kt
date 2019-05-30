package org.imperiumlabs.geofirestore.callbacks

import com.google.firebase.firestore.DocumentSnapshot

/**
 * SingleGeoQuery notify the listeners with this interface about the success or failure of the query.
 */
interface SingleGeoQueryDataEventCallback {

    /**
     * Called if SingleGeoQuery has successfully obtained some data.
     *
     * @param documentSnapshots List of snapshots associated with the obtained documents.
     */
    fun onSuccess(documentSnapshots: List<DocumentSnapshot>)

    /**
     * Called in case an exception occurred while retrieving locations for a query.
     *
     * @param exception The exception that occurred while retrieving the query.
     */
    fun onError(exception: Exception)
}