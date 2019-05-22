package org.imperiumlabs.geofirestore

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import org.imperiumlabs.geofirestore.callbacks.SingleGeoQueryDataEventListener
import org.imperiumlabs.geofirestore.core.GeoHashQuery
import org.imperiumlabs.geofirestore.extension.mapNotNullManyTo

/**
 * A SingleGeoQuery object can be used for a geo query of one-shot type,
 * it fires only one time and notify every listener attached to it
 */
class SingleGeoQuery(
        //The instance of GeoFirestore
        private val mGeoFirestore: GeoFirestore,
        //The GeoPoint to center the query
        private val mCenterPoint: GeoPoint,
        //The radius of search
        private val mRadius: Double) {

    //The setupQuery method has already been called?
    private var mGeoQueryInitialized = false
    //Store all the listeners
    private val mEventListeners = arrayListOf<SingleGeoQueryDataEventListener>()
    //Store the data previously sent in order to notify immediately
    private val mOldData = arrayListOf<DocumentSnapshot>()

    /*
     * Get the GeoHashQuery for a location and a radius and for each
     * resulting Query get the DocumentSnapshot.
     *
     * Every Task is saved and the result are awaited all together
     * in order to reduce the calls to the listeners.
     */
    private fun setupQuery() {
        mGeoQueryInitialized = true

        //Get the resultTasks from Firebase Queries generated from GeoHashQueries
        val resultTasks = arrayListOf<Task<QuerySnapshot>>().apply {
            GeoHashQuery.queriesAtLocation(
                    GeoLocation(mCenterPoint.latitude, mCenterPoint.longitude),
                    mRadius
            ).forEach { this.add(it.createFirestoreQuery().get()) }
        }

        //Await the completion of all the resultTasks
        Tasks.whenAllComplete(resultTasks)
                .addOnFailureListener { e ->
                    //Some error occurred, notify it to the listeners
                    GeoFirestore.LOGGER.warning("Failed retrieving data for geo query")
                    mEventListeners.forEach { it.onError(e) }
                }
                .addOnSuccessListener { tasks ->
                    //Data retrieved, extract it from the tasks and pass it to the listeners
                    val documentSnapshots = arrayListOf<DocumentSnapshot>()
                    tasks.mapNotNullManyTo(documentSnapshots) { (it.result as? QuerySnapshot)?.documents }
                    mEventListeners.forEach { it.onSuccess(documentSnapshots) }
                }
    }

    /*
     * Extension function used to create a Firebase Query
     * from a GeoHashQuery and the GeoFirestore instance
     * of this class.
     *
     * This method is private and works only inside this class.
     */
    private fun GeoHashQuery.createFirestoreQuery() =
            mGeoFirestore.collectionReference
                    .orderBy("g")
                    .startAt(this.startValue)
                    .endAt(this.endValue)

    /*
     * Reset the SingleGeoQuery clearing the old data
     */
    private fun reset() {
        mGeoQueryInitialized = false
        mOldData.clear()
    }

    /**
     * Get the Firestore query(s) for this SingleGeoQuery
     *
     * @return A list of Firestore Query(s)
     */
    fun getQueries() = arrayListOf<Query>().apply {
        GeoHashQuery.queriesAtLocation(
                GeoLocation(mCenterPoint.latitude, mCenterPoint.longitude),
                mRadius
        ).forEach { this.add(it.createFirestoreQuery()) }
    }

    /**
     * Add a SingleGeoQueryDataEventListener
     */
    fun addSingleGeoQueryEventListener(listener: SingleGeoQueryDataEventListener) {
        if (mEventListeners.contains(listener))
            throw IllegalArgumentException("Added the same listener twice to a SingleGeoQuery!")
        //Add the listener to the others
        mEventListeners.add(listener)
        //Check if there are some data previously crated
        if (mOldData.isNotEmpty())
            mEventListeners.forEach { it.onSuccess(mOldData) }
        //If the query is not initialized initialize it
        if (!mGeoQueryInitialized)
            setupQuery()
    }

    /**
     * Remove a SingleGeoQueryDataEventListener
     */
    fun removeSingleGeoQueryEventListener(listener: SingleGeoQueryDataEventListener) {
        if (!mEventListeners.contains(listener))
            throw IllegalArgumentException("Trying to remove listener that was removed or not added!")
        mEventListeners.remove(listener)
        //If there are no more listeners we reset the query
        if (mEventListeners.isEmpty())
            reset()
    }

    /**
     * Remove all the attached listeners
     */
    fun removeAllListeners() {
        mEventListeners.clear()
        reset()
    }
}