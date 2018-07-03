package com.imperiumlabs.geofirestore;

import com.google.firebase.firestore.*;
import com.imperiumlabs.geofirestore.core.GeoHash;
import com.imperiumlabs.geofirestore.util.GeoUtils;
import java.lang.Throwable;
import java.util.*;
import java.util.logging.Logger;

/**
 * A GeoFirestore instance is used to store geo location data in Firestore.
 */

// COMPLETED; HOWEVER: Confirm setLocation, removeLocation, getLocation are implemented correctly
public class GeoFirestore {
    public static Logger LOGGER = Logger.getLogger("GeoFirestore");

    /**
     * A listener that can be used to be notified about a successful write or an error on writing.
     */

    // COMPLETED
    public interface CompletionListener {
        /**
         * Called once a location was successfully saved on the server or an error occurred. On success, the parameter
         * error will be null; in case of an error, the error will be passed to this method.
         *
         * @param documentID The documentID of the document whose location was saved
         * @param exception The exception or null if no exception occurred
         */
        void onComplete(String documentID, Exception exception);
    }

    /**
     * A callback that can be used to retrieve a location or an error in retrieving a location.
     */

    // COMPLETED
    public interface LocationCallback {
        /**
         * Called once a location is fetched from the server. On success, the parameter
         * error will be null; in case of an error, the error will be passed to this method.
         *
         * @param location The location fetched from the server
         * @param exception The exception or null if no exception occurred
         */
        void onComplete(GeoPoint location, Exception exception);
    }

    // COMPLETED
    public class LocationException extends Exception {
        public LocationException() { super(); }
        public LocationException(String message) { super(message); }
        public LocationException(String message, Throwable cause) { super(message, cause); }
        public LocationException(Throwable cause) { super(cause); }
    }

    // COMPLETED
    public static GeoPoint getLocationValue(DocumentSnapshot documentSnapshot) {
        try {
            Map<String, Object> data = documentSnapshot.getData();
            List<?> location = (List<?>) data.get("l");
            Number latitudeObj = (Number) location.get(0);
            Number longitudeObj = (Number) location.get(1);
            double latitude = latitudeObj.doubleValue();
            double longitude = longitudeObj.doubleValue();
            if (location.size() == 2 && GeoLocation.coordinatesValid(latitude, longitude)) {
                return new GeoPoint(latitude, longitude);
            } else {
                return null;
            }
        } catch (NullPointerException e) {
            return null;
        } catch (ClassCastException e) {
            return null;
        }
    }

    // COMPLETED
    private final CollectionReference collectionReference;
    private final EventRaiser eventRaiser;

    /**
     * Creates a new GeoFirestore instance at the given Firestore collection reference.
     *
     * @param collectionReference The Firestore collection reference this GeoFirestore instance uses
     */

    // COMPLETED
    public GeoFirestore(CollectionReference collectionReference) {
        this.collectionReference = collectionReference;
        EventRaiser eventRaiser;
        try {
            eventRaiser = new AndroidEventRaiser();
        } catch (Throwable e) {
            // We're not on Android, use the ThreadEventRaiser
            eventRaiser = new ThreadEventRaiser();
        }
        this.eventRaiser = eventRaiser;
    }

    /**
     * @return The Firestore collection reference this GeoFirestore instance uses
     */

    // COMPLETED
    public CollectionReference getCollectionReference() {
        return this.collectionReference;
    }

    // COMPLETED
    DocumentReference getRefForDocumentID(String documentID) {
        return this.collectionReference.document(documentID);
    }

    /**
     * Sets the location of a document.
     *
     * @param documentID The documentID of the document to save the location for
     * @param location The location of this document
     */

    // COMPLETED
    public void setLocation(String documentID, GeoPoint location) {
        this.setLocation(documentID, location, null);
    }

    /**
     * Sets the location of a document.
     *
     * @param documentID The documentID of the document to save the location for
     * @param location The location of this document
     * @param completionListener A listener that is called once the location was successfully saved on the server or an
     *                           error occurred
     */

    // COMPLETED
    public void setLocation(final String documentID, final GeoPoint location, final CompletionListener completionListener) {
        if (documentID == null) {
            completionListener.onComplete(documentID, new NullPointerException());
            return;
        }
        DocumentReference docRef = this.getRefForDocumentID(documentID);
        GeoHash geoHash = new GeoHash(new GeoLocation(location.getLatitude(), location.getLongitude()));
        Map<String, Object> updates = new HashMap<>();
        updates.put("g", geoHash.getGeoHashString());
        updates.put("l", Arrays.asList(location.getLatitude(), location.getLongitude()));
        Exception exception = docRef.set(updates, SetOptions.merge()).getException();
        if (completionListener != null) {
            completionListener.onComplete(documentID, exception);
        }
    }

    /**
     * Removes the location of a document from this GeoFirestore instance.
     *
     * @param documentID The documentID of the document to remove from this GeoFirestore instance
     */
    public void removeLocation(String documentID) {
        this.removeLocation(documentID, null);
    }

    /**
     * Removes the location of a document from this GeoFirestore.
     *
     * @param documentID The documentID of the document to remove from this GeoFirestore
     * @param completionListener A completion listener that is called once the location is successfully removed
     *                           from the server or an error occurred
     */

    // COMPLETED
    public void removeLocation(final String documentID, final CompletionListener completionListener) {
        if (documentID == null) {
            completionListener.onComplete(documentID, new NullPointerException());
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("g", FieldValue.delete());
        updates.put("l", FieldValue.delete());
        DocumentReference docRef = this.getRefForDocumentID(documentID);
        Exception exception = docRef.set(updates, SetOptions.merge()).getException();
        if (completionListener != null) {
            completionListener.onComplete(documentID, exception);
        }
    }

    /**
     * Gets the current location for a document and calls the callback with the current value.
     *
     * @param documentID The documentID of the document whose location to get
     * @param callback The callback that is called once the location is retrieved
     */

    // COMPLETED
    public void getLocation(String documentID, LocationCallback callback) {
        try {
            DocumentSnapshot documentSnapshot = this.getRefForDocumentID(documentID).get().getResult();
            Map<String, Object> data = documentSnapshot.getData();
            List<?> location = (List<?>) data.get("l");
            Number latitudeObj = (Number) location.get(0);
            Number longitudeObj = (Number) location.get(1);
            double latitude = latitudeObj.doubleValue();
            double longitude = longitudeObj.doubleValue();
            if (location.size() == 2 && GeoLocation.coordinatesValid(latitude, longitude)) {
                callback.onComplete(new GeoPoint(latitude, longitude), null);
            } else {
                callback.onComplete(null, new LocationException("Invalid Location"));
            }
        } catch (NullPointerException e) {
            callback.onComplete(null, e);
        } catch (ClassCastException e) {
            callback.onComplete(null, e);
        }
    }

    /**
     * Returns a new Query object centered at the given location and with the given radius.
     *
     * @param center The center of the query
     * @param radius The radius of the query, in kilometers. The maximum radius that is
     * supported is about 8587km. If a radius bigger than this is passed we'll cap it.
     * @return The new GeoQuery object
     */

    // COMPLETED
    public GeoQuery queryAtLocation(GeoPoint center, double radius) {
        return new GeoQuery(this, center, GeoUtils.capRadius(radius));
    }

    // COMPLETED
    public void raiseEvent(Runnable r) {
        this.eventRaiser.raiseEvent(r);
    }
}
