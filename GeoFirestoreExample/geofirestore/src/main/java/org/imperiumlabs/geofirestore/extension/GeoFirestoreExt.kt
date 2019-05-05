package org.imperiumlabs.geofirestore.extension

import com.google.firebase.firestore.GeoPoint
import org.imperiumlabs.geofirestore.GeoFirestore
/*
 * This file contains a series of extension functions
 * used to differentiate the Kotlin lambda version of
 * the same functions in the GeoFirestore class
 */
/**
 * Gets the current location for a document and calls the callback with the current value.
 *
 * @param documentID The documentID of the document whose location to get
 * @param callback The Lambda function that is called once the location is retrieved
 */
fun GeoFirestore.getLocation(documentID: String, callback: (location: GeoPoint?, exception: Exception?)->Unit) {
    this.getLocation(documentID, object : GeoFirestore.LocationCallback {
        override fun onComplete(location: GeoPoint?, exception: Exception?) {
            callback(location, exception)
        }
    })
}

/**
 * Removes the location of a document from this GeoFirestore.
 *
 * @param documentID The documentID of the document to remove from this GeoFirestore
 * @param completionListener A lambda function that is called once the location is successfully removed
 *                           from the server or an error occurred
 */
fun GeoFirestore.removeLocation(documentID: String?, completionListener: (exception: Exception?)->Unit) {
    this.removeLocation(documentID, object : GeoFirestore.CompletionListener {
        override fun onComplete(exception: Exception?) {
            completionListener(exception)
        }
    })
}

/**
 * Sets the location of a document.
 *
 * @param documentID The documentID of the document to save the location for
 * @param location The location of this document
 * @param completionListener Lambda function called when the location was successfully saved on the server
 *                           or an error occurred
 */
fun GeoFirestore.setLocation(documentID: String?, location: GeoPoint, completionListener: (exception: Exception?)->Unit) {
    this.setLocation(documentID, location, object : GeoFirestore.CompletionListener {
        override fun onComplete(exception: Exception?) {
            completionListener(exception)
        }
    })
}