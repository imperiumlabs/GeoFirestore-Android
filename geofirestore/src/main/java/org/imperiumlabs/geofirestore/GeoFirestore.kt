package org.imperiumlabs.geofirestore

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import org.imperiumlabs.geofirestore.core.GeoHash
import org.imperiumlabs.geofirestore.core.GeoHashQuery
import org.imperiumlabs.geofirestore.extension.mapNotNullManyTo
import org.imperiumlabs.geofirestore.util.GeoUtils
import java.util.logging.Logger


/**
 * A GeoFirestore instance is used to store geo location data in Firestore.
 */
class GeoFirestore(val collectionReference: CollectionReference) {

    companion object {
        @JvmField
        val LOGGER = Logger.getLogger("GeoFirestore")!!

        /**
         * Build a GeoPoint from a DocumentSnapshot
         *
         * This model takes as a input a DocumentSnapshot and tries to extract the parameter "l",
         * if it is of type List we extract latitude and longitude and create a valid GeoPoint,
         * if it is already a GeoPoint we return it, in every other case we return null.
         *
         * @param documentSnapshot The DocumentSnapshot from which to get the Location Data
         * @return Nullable GeoPoint with the location of the documentSnapshot
         */
        fun getLocationValue(documentSnapshot: DocumentSnapshot): GeoPoint? {
            return try {
                when (val locationDataRaw = documentSnapshot.data!!["l"]) {
                    is List<*> -> {
                        val latitudeObj = locationDataRaw[0] as Double
                        val longitudeObj = locationDataRaw[1] as Double
                        if (locationDataRaw.size == 2 && GeoLocation.coordinatesValid(latitudeObj, longitudeObj))
                            GeoPoint(latitudeObj, longitudeObj)
                        else null
                    }
                    is GeoPoint -> locationDataRaw
                    else -> null
                }
            } catch (e: NullPointerException) {
                null
            } catch (e: ClassCastException) {
                null
            }
        }
    }

    /**
     * A callback that can be used to be notified about a successful write or an error on writing.
     */
    interface CompletionCallback {
        /**
         * Called once a location was successfully saved on the server or an error occurred. On success, the parameter
         * error will be null; in case of an error, the error will be passed to this method.
         *
         * @param exception The exception or null if no exception occurred
         */
        fun onComplete(exception: Exception?)
    }

    /**
     * A callback that can be used to retrieve a location or an error.
     */
    interface LocationCallback {
        /**
         * Called once a location is fetched from the server. On success, the parameter
         * error will be null; in case of an error, the error will be passed to this method.
         *
         * @param location The location fetched from the server
         * @param exception The exception or null if no exception occurred
         */
        fun onComplete(location: GeoPoint?, exception: Exception?)
    }

    /**
     * A callback that can be used to retrieve the documents or an error.
     */
    interface SingleGeoQueryDataEventCallback {

        /**
         * Called once some data is obtained from a query. On success, the parameter
         * error will be null; in case of an error, the error will be passed to this.
         *
         * @param documentSnapshots List of snapshots associated with the obtained documents.
         * @param exception The exception or null if no exception occurred
         */
        fun onComplete(documentSnapshots: List<DocumentSnapshot>?, exception: Exception?)
    }

    //Instance of the EventRaiser
    private var mEventRaiser: EventRaiser

    init {
        try {
            this.mEventRaiser = AndroidEventRaiser()
        } catch (e: Throwable) {
            // We're not on Android, use the ThreadEventRaiser
            this.mEventRaiser = ThreadEventRaiser()
        }
    }

    /**
     * @param documentID The documentID of the document to retrieve the DocumentReference
     * @return DocumentReference for the given documentID
     */
    fun getRefForDocumentID(documentID: String) = this.collectionReference.document(documentID)

    /**
     * Sets the location of a document.
     *
     * @param documentID The documentID of the document to save the location for
     * @param location The location of this document
     */
    fun setLocation(documentID: String?, location: GeoPoint) {
        this.setLocation(documentID, location, null)
    }

    /**
     * Sets the location of a document.
     *
     * @param documentID The documentID of the document to save the location for
     * @param location The location of this document
     * @param completionCallback A listener that is called once the location was successfully saved on the server
     *                           or an error occurred
     */
    fun setLocation(documentID: String?, location: GeoPoint, completionCallback: CompletionCallback?) {
        if (documentID == null) {
            completionCallback?.onComplete(NullPointerException("Document ID is null"))
            return
        }
        //Get the DocumentReference for this documentID
        val docRef = this.getRefForDocumentID(documentID)
        val geoHash = GeoHash(GeoLocation(location.latitude, location.longitude))
        //Create a Map with the fields to add
        val updates = HashMap<String, Any>()
        updates["g"] = geoHash.geoHashString
        updates["l"] = location
        //Update the DocumentReference with the location data
        docRef.set(updates, SetOptions.merge())
                .addOnSuccessListener { completionCallback?.onComplete(null) }
                .addOnFailureListener { completionCallback?.onComplete(it) }
    }

    /**
     * Removes the location of a document from this GeoFirestore instance.
     *
     * @param documentID The documentID of the document to remove from this GeoFirestore instance
     */
    fun removeLocation(documentID: String?) {
        this.removeLocation(documentID, null)
    }

    /**
     * Removes the location of a document from this GeoFirestore.
     *
     * @param documentID The documentID of the document to remove from this GeoFirestore
     * @param completionCallback A completion listener that is called once the location is successfully removed
     *                           from the server or an error occurred
     */
    fun removeLocation(documentID: String?, completionCallback: CompletionCallback?) {
        if (documentID == null) {
            completionCallback?.onComplete(NullPointerException("Document ID is null"))
            return
        }
        //Crate a Map with the fields to remove
        val updates = HashMap<String, Any>()
        updates["g"] = FieldValue.delete()
        updates["l"] = FieldValue.delete()
        //Remove the relative locations fields from the DocumentReference
        val docRef = this.getRefForDocumentID(documentID)
        docRef.set(updates, SetOptions.merge())
                .addOnSuccessListener { completionCallback?.onComplete(null) }
                .addOnFailureListener { completionCallback?.onComplete(it) }
    }

    /**
     * Gets the current location for a document and calls the callback with the current value.
     *
     * @param documentID The documentID of the document whose location to get
     * @param callback The callback that is called once the location is retrieved
     */
    fun getLocation(documentID: String, callback: LocationCallback) {
        this.getRefForDocumentID(documentID).get()
                .addOnFailureListener { callback.onComplete(location = null, exception = it) }
                .addOnSuccessListener { snap ->
                    getLocationValue(snap).also { geoPoint ->
                        if (geoPoint != null)
                            callback.onComplete(location = geoPoint, exception = null)
                        else
                            callback.onComplete(location = null, exception = NullPointerException("Location doesn't exist"))
                    }
                }
    }

    /**
     * Returns a new Query object centered at the given location and with the given radius.
     *
     * @param center The center of the query
     * @param radius The radius of the query, in kilometers. The maximum radius that is
     *               supported is about 8587km. If a radius bigger than this is passed we'll cap it.
     * @return The new GeoQuery object
     */
    fun queryAtLocation(center: GeoPoint, radius: Double) = GeoQuery(this, center, GeoUtils.capRadius(radius))

    /**
     * Returns a new SingleGeoQuery object centered at a given location and with the given radius.
     *
     * @param center The center of the query
     * @param radius The radius of the query, in kilometers. The maximum radius that is
     *               supported is about 8587km. If a radius bigger than this is passed we'll cap it.
     * @return The new SingleGeoQuery object
     */
    fun getAtLocation(center: GeoPoint, radius: Double, callback: SingleGeoQueryDataEventCallback) {
        //Get the resultTasks from Firebase Queries generated from GeoHashQueries
        val resultTasks = arrayListOf<Task<QuerySnapshot>>().apply {
            GeoHashQuery.queriesAtLocation(GeoLocation(center.latitude, center.longitude), radius)
                    .forEach {
                        this.add(this@GeoFirestore.collectionReference
                                .orderBy("g")
                                .startAt(it.startValue)
                                .endAt(it.endValue)
                                .get())
                    }
        }
        //Await the completion of all the resultTasks
        Tasks.whenAllComplete(resultTasks)
                .addOnFailureListener { e ->
                    //Some error occurred, notify it to the listeners
                    GeoFirestore.LOGGER.warning("Failed retrieving data for geo query")
                    callback.onComplete(null, e)
                }
                .addOnSuccessListener { tasks ->
                    //Data retrieved, extract it from the tasks and pass it to the listeners
                    val documentSnapshots = arrayListOf<DocumentSnapshot>()
                    tasks.mapNotNullManyTo(documentSnapshots) { (it.result as? QuerySnapshot)?.documents }
                    callback.onComplete(documentSnapshots, null)
                }
    }

    /**
     * Raise an event from the EventRaiser
     *
     * @param r The Runnable to pass to the EventRaiser
     */
    fun raiseEvent(r: Runnable) { this.mEventRaiser.raiseEvent(r) }
}
