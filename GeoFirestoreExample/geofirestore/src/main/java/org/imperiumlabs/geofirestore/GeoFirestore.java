package org.imperiumlabs.geofirestore;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.*;
import android.support.annotation.NonNull;
import org.imperiumlabs.geofirestore.core.GeoHash;
import org.imperiumlabs.geofirestore.util.GeoUtils;
import java.lang.Throwable;
import java.util.*;
import java.util.logging.Logger;

/**
 * A GeoFirestore instance is used to store geo location data in Firestore.
 */
public class GeoFirestore {

    public static Logger LOGGER = Logger.getLogger("GeoFirestore");

    /**
     * A listener that can be used to be notified about a successful write or an error on writing.
     */
    public interface CompletionListener {
        /**
         * Called once a location was successfully saved on the server or an error occurred. On success, the parameter
         * error will be null; in case of an error, the error will be passed to this method.
         *
         * @param exception The exception or null if no exception occurred
         */
        void onComplete(Exception exception);
    }

    /**
     * A callback that can be used to retrieve a location or an error in retrieving a location.
     */
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

    private final CollectionReference collectionReference;
    private final EventRaiser eventRaiser;

    /**
     * Creates a new GeoFirestore instance at the given Firestore collection reference.
     *
     * @param collectionReference The Firestore collection reference this GeoFirestore instance uses
     */
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
    public CollectionReference getCollectionReference() {
        return this.collectionReference;
    }

    DocumentReference getRefForDocumentID(String documentID) {
        return this.collectionReference.document(documentID);
    }

    /**
     * Sets the location of a document.
     *
     * @param documentID The documentID of the document to save the location for
     * @param location The location of this document
     */
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
    public void setLocation(final String documentID, final GeoPoint location, final CompletionListener completionListener) {
        if (documentID == null) {
            completionListener.onComplete(new NullPointerException("Document ID is null"));
            return;
        }
        DocumentReference docRef = this.getRefForDocumentID(documentID);
        GeoHash geoHash = new GeoHash(new GeoLocation(location.getLatitude(), location.getLongitude()));
        Map<String, Object> updates = new HashMap<>();
        updates.put("g", geoHash.getGeoHashString());
        updates.put("l", Arrays.asList(location.getLatitude(), location.getLongitude()));
        docRef.set(updates, SetOptions.merge()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (completionListener != null){
                    if (task.isSuccessful()){
                        completionListener.onComplete(null);
                    }else{
                        completionListener.onComplete(task.getException());
                    }
                }
            }
        });
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
    public void removeLocation(final String documentID, final CompletionListener completionListener) {
        if (documentID == null) {
            completionListener.onComplete(new NullPointerException("Document ID is null"));
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("g", FieldValue.delete());
        updates.put("l", FieldValue.delete());
        DocumentReference docRef = this.getRefForDocumentID(documentID);
        docRef.set(updates, SetOptions.merge()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (completionListener != null){
                    if (task.isSuccessful()){
                        completionListener.onComplete(null);
                    }else{
                        completionListener.onComplete(task.getException());
                    }
                }
            }
        });
    }

    /**
     * Gets the current location for a document and calls the callback with the current value.
     *
     * @param documentID The documentID of the document whose location to get
     * @param callback The callback that is called once the location is retrieved
     */
    public void getLocation(String documentID, final LocationCallback callback) {
        this.getRefForDocumentID(documentID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {

                    DocumentSnapshot document = task.getResult();
                    GeoPoint location = getLocationValue(document);
                    if (location != null) {
                        callback.onComplete(location, null);
                    } else {
                        callback.onComplete(null, new NullPointerException("Location doesn't exist"));
                    }

                } else {
                    callback.onComplete(null, task.getException());
                }
            }
        });
    }

    /**
     * Returns a new Query object centered at the given location and with the given radius.
     *
     * @param center The center of the query
     * @param radius The radius of the query, in kilometers. The maximum radius that is
     * supported is about 8587km. If a radius bigger than this is passed we'll cap it.
     * @return The new GeoQuery object
     */
    public GeoQuery queryAtLocation(GeoPoint center, double radius) {
        return new GeoQuery(this, center, GeoUtils.capRadius(radius));
    }

    public void raiseEvent(Runnable r) {
        this.eventRaiser.raiseEvent(r);
    }
}
