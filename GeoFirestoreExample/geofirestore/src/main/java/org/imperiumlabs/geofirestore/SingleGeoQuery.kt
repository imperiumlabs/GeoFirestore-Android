package org.imperiumlabs.geofirestore

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.QuerySnapshot
import org.imperiumlabs.geofirestore.callbacks.SingleGeoQueryDataEventListener
import org.imperiumlabs.geofirestore.core.GeoHashQuery
import org.imperiumlabs.geofirestore.extension.mapNotNullManyTo

/**
 * A SingleGeoQuery object can be used for a geo query of one-shot type,
 * it fires only one time and notify every listener attached to it
 */
class SingleGeoQuery(private val geoFirestore: GeoFirestore,
                     private val center: GeoPoint,
                     private val radius: Double) {

    //The setupQuery method has already been called?
    private var geoQueryInitialized = false
    //Store all the listeners
    private val eventListeners = arrayListOf<SingleGeoQueryDataEventListener>()

    /*
     * Get the GeoHashQuery for a location and a radius and for each
     * resulting Query get the DocumentSnapshot.
     *
     * Every Task is saved and the result are awaited all together
     * in order to reduce the calls to the listeners.
     */
    private fun setupQuery() {
        geoQueryInitialized = true

        val queries = GeoHashQuery.queriesAtLocation(GeoLocation(center.latitude, center.longitude), radius)
        val resultTask = arrayListOf<Task<QuerySnapshot>>()

        for (query in queries) {
            val firestoreQuery = geoFirestore.collectionReference
                    .orderBy("g")
                    .startAt(query.startValue)
                    .endAt(query.endValue)
            resultTask.add(firestoreQuery.get())
        }

        Tasks.whenAllComplete(resultTask)
                .addOnFailureListener { e ->
                    GeoFirestore.LOGGER.warning("Failed retrieving data for geo query")
                    eventListeners.forEach { it.onError(e) }
                }
                .addOnSuccessListener { tasks ->
                    val documentSnapshots = arrayListOf<DocumentSnapshot>()
                    tasks.mapNotNullManyTo(documentSnapshots) { (it.result as? QuerySnapshot)?.documents }
                    eventListeners.forEach { it.onSuccess(documentSnapshots) }
                }
    }

    /**
     * Add a SingleGeoQueryDataEventListener
     */
    fun addSingleGeoQueryEventListener(listener: SingleGeoQueryDataEventListener) {
        if (eventListeners.contains(listener))
            throw IllegalArgumentException("Added the same listener twice to a SingleGeoQuery!")
        eventListeners.add(listener)
        if (!geoQueryInitialized)
            setupQuery()
    }

    /**
     * Remove a SingleGeoQueryDataEventListener
     */
    fun removeSingleGeoQueryEventListener(listener: SingleGeoQueryDataEventListener) {
        if (!eventListeners.contains(listener))
            throw IllegalArgumentException("Trying to remove listener that was removed or not added!")
        eventListeners.remove(listener)
        if (!hasListeners())
            reset()
    }

    /**
     * Remove all the attached listeners
     */
    fun removeAllListeners() {
        eventListeners.clear()
        reset()
    }

    private fun hasListeners() = eventListeners.isNotEmpty()

    private fun reset() {
        geoQueryInitialized = false
    }
}