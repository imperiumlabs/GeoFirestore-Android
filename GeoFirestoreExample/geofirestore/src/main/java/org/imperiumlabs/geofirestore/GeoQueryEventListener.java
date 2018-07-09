package org.imperiumlabs.geofirestore;

import com.google.firebase.firestore.GeoPoint;

import java.lang.Exception;

// FULLY TESTED

/**
 * GeoQuery notifies listeners with this interface about documentIDs that entered, exited, or moved within the query.
 */
public interface GeoQueryEventListener {

    /**
     * Called if a documentID entered the search area of the GeoQuery. This method is called for every documentID currently in the
     * search area at the time of adding the listener.
     *
     * This method is once per documentID, and is only called again if onKeyExited was called in the meantime.
     *
     * @param documentID The documentID that entered the search area
     * @param location The location for this documentID
     */
    void onKeyEntered(String documentID, GeoPoint location);

    /**
     * Called if a documentID exited the search area of the GeoQuery. This method is only called if onKeyEntered was called
     * for the documentID.
     *
     * @param documentID The documentID that exited the search area
     */
    void onKeyExited(String documentID);

    /**
     * Called if a documentID moved within the search area.
     *
     * This method can be called multiple times.
     *
     * @param documentID The documentID that moved within the search area
     * @param location The location for this documentID
     */
    void onKeyMoved(String documentID, GeoPoint location);

    /**
     * Called once all initial GeoFirestore data has been loaded and the relevant events have been fired for this query.
     * Every time the query criteria is updated, this observer will be called after the updated query has fired the
     * appropriate key entered or key exited events.
     */
    void onGeoQueryReady();

    /**
     * Called in case an exception occurred while retrieving locations for a query, e.g. violating security rules.
     * @param exception The exception that occurred while retrieving the query
     */
    void onGeoQueryError(Exception exception);

}
