package org.imperiumlabs.geofirestore;


// FULLY TESTED

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.imperiumlabs.geofirestore.listeners.EventListenerBridge;
import org.imperiumlabs.geofirestore.listeners.GeoQueryDataEventListener;
import org.imperiumlabs.geofirestore.listeners.GeoQueryEventListener;
import org.imperiumlabs.geofirestore.core.GeoHash;
import org.imperiumlabs.geofirestore.core.GeoHashQuery;
import org.imperiumlabs.geofirestore.util.GeoUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

// TODO: 05/05/19 Android Studio show error for javadoc in @throws IllegalArgumentException
/**
 * A GeoQuery object can be used for geo queries in a given circle. The GeoQuery class is thread safe.
 */
public class GeoQuery {
    private static final int KILOMETER_TO_METER = 1000;

    private static class LocationInfo {
        final GeoPoint location;
        final boolean inGeoQuery;
        final GeoHash geoHash;
        final DocumentSnapshot documentSnapshot;

        LocationInfo(GeoPoint location, boolean inGeoQuery, DocumentSnapshot documentSnapshot) {
            this.location = location;
            this.inGeoQuery = inGeoQuery;
            this.geoHash = new GeoHash(new GeoLocation(location.getLatitude(), location.getLongitude()));
            this.documentSnapshot = documentSnapshot;
        }
    }

    private static class GeoHashQueryListener {
        final ListenerRegistration childAddedListener;
        final ListenerRegistration childRemovedListener;
        final ListenerRegistration childChangedListener;

        GeoHashQueryListener(ListenerRegistration childAddedListener,
                             ListenerRegistration childRemovedListener,
                             ListenerRegistration childChangedListener){
            this.childAddedListener = childAddedListener;
            this.childRemovedListener = childRemovedListener;
            this.childChangedListener = childChangedListener;
        }
    }

    private final GeoFirestore geoFirestore;

    private final Map<String, LocationInfo> locationInfos = new HashMap<>();
    private Set<GeoHashQuery> queries;
    private final Map<GeoHashQuery, Query> firestoreQueries = new HashMap<>();
    private final Map<GeoHashQuery, GeoHashQueryListener> handles = new HashMap<>();
    private final Set<GeoHashQuery> outstandingQueries = new HashSet<>();

    private final Set<GeoQueryDataEventListener> eventListeners = new HashSet<>();

    private GeoPoint center;
    private double radius;


    /**
     * Creates a new GeoQuery object centered at the given location and with the given radius.
     * @param geoFirestore The GeoFirestore object this GeoQuery uses
     * @param center The center of this query
     * @param radius The radius of the query, in kilometers. The maximum radius that is
     * supported is about 8587km. If a radius bigger than this is passed we'll cap it.
     */
    GeoQuery(GeoFirestore geoFirestore, GeoPoint center, double radius) {
        this.geoFirestore = geoFirestore;
        this.center = center;
        this.radius = radius * KILOMETER_TO_METER; // Convert from kilometers to meters.
    }

    private boolean locationIsInQuery(GeoPoint location) {
        return GeoUtils.INSTANCE.distance(new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoLocation(center.getLatitude(), center.getLongitude())) <= this.radius;
    }

    private void updateLocationInfo(final DocumentSnapshot documentSnapshot, final GeoPoint location) {
        String documentID = documentSnapshot.getId();
        LocationInfo oldInfo = this.locationInfos.get(documentID);

        boolean isNew = oldInfo == null;
        final boolean changedLocation = oldInfo != null && !oldInfo.location.equals(location);
        boolean wasInQuery = oldInfo != null && oldInfo.inGeoQuery;

        boolean isInQuery = this.locationIsInQuery(location);
        if ((isNew || !wasInQuery) && isInQuery) {
            for (final GeoQueryDataEventListener listener: this.eventListeners) {
                this.geoFirestore.raiseEvent(new Runnable() {
                    @Override
                    public void run() {
                        listener.onDocumentEntered(documentSnapshot, location);
                    }
                });
            }
        } else if (!isNew && isInQuery) {
            for (final GeoQueryDataEventListener listener: this.eventListeners) {
                this.geoFirestore.raiseEvent(new Runnable() {
                    @Override
                    public void run() {
                        if (changedLocation) {
                            listener.onDocumentMoved(documentSnapshot, location);
                        }

                        listener.onDocumentChanged(documentSnapshot, location);
                    }
                });
            }
        } else if (wasInQuery && !isInQuery) {
            for (final GeoQueryDataEventListener listener: this.eventListeners) {
                this.geoFirestore.raiseEvent(new Runnable() {
                    @Override
                    public void run() {
                        listener.onDocumentExited(documentSnapshot);
                    }
                });
            }
        }
        LocationInfo newInfo = new LocationInfo(location, this.locationIsInQuery(location), documentSnapshot);
        this.locationInfos.put(documentID, newInfo);
    }

    private boolean geoHashQueriesContainGeoHash(GeoHash geoHash) {
        if (this.queries == null) {
            return false;
        }
        for (GeoHashQuery query: this.queries) {
            if (query.containsGeoHash(geoHash)) {
                return true;
            }
        }
        return false;
    }

    private void reset() {
        for(Map.Entry<GeoHashQuery, Query> entry: this.firestoreQueries.entrySet()) {
            GeoHashQueryListener handle = handles.get(entry.getKey());
            handle.childAddedListener.remove();
            handle.childRemovedListener.remove();
            handle.childChangedListener.remove();
        }

        this.locationInfos.clear();
        this.queries = null;
        this.firestoreQueries.clear();
        this.handles.clear();
        this.outstandingQueries.clear();
    }

    private boolean hasListeners() {
        return !this.eventListeners.isEmpty();
    }

    private boolean canFireReady() {
        return this.outstandingQueries.isEmpty();
    }

    private void checkAndFireReady() {
        if (canFireReady()) {
            for (final GeoQueryDataEventListener listener: this.eventListeners) {
                this.geoFirestore.raiseEvent(new Runnable() {
                    @Override
                    public void run() {
                        listener.onGeoQueryReady();
                    }
                });
            }
        }
    }

    private void addValueToReadyListener(final Query firestore, final GeoHashQuery query) {
        firestore.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull final Task<QuerySnapshot> task) {
                if (task.isSuccessful()){

                    synchronized (GeoQuery.this) {
                        GeoQuery.this.outstandingQueries.remove(query);
                        GeoQuery.this.checkAndFireReady();
                    }

                }else{

                    synchronized (GeoQuery.this) {
                        for (final GeoQueryDataEventListener listener : GeoQuery.this.eventListeners) {
                            GeoQuery.this.geoFirestore.raiseEvent(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onGeoQueryError(task.getException());
                                }
                            });
                        }
                    }

                }
            }
        });
    }

    private void setupQueries() {
        Set<GeoHashQuery> oldQueries = (queries == null) ? new HashSet<GeoHashQuery>() : queries;
        Set<GeoHashQuery> newQueries = GeoHashQuery.Companion.queriesAtLocation(new GeoLocation(center.getLatitude(), center.getLongitude()), radius);
        this.queries = newQueries;

        for (GeoHashQuery query: oldQueries) {
            if (!newQueries.contains(query)) {
                GeoHashQueryListener handle = handles.get(firestoreQueries.get(query));
                if (handle != null) {
                    handle.childAddedListener.remove();
                    handle.childRemovedListener.remove();
                    handle.childChangedListener.remove();
                }
                firestoreQueries.remove(query);
                outstandingQueries.remove(query);
            }
        }
        for (final GeoHashQuery query: newQueries) {
            if (!oldQueries.contains(query)) {
                outstandingQueries.add(query);
                CollectionReference collectionReference = this.geoFirestore.getCollectionReference();

                Query firestoreQuery = collectionReference.orderBy("g").startAt(query.getStartValue()).endAt(query.getEndValue());

                ListenerRegistration childAddedListener = firestoreQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (queryDocumentSnapshots != null && e == null){
                            for(final DocumentChange docChange: queryDocumentSnapshots.getDocumentChanges()){
                                if (docChange.getType() == DocumentChange.Type.ADDED){
                                    childAdded(docChange.getDocument());
                                }
                            }
                        }
                    }
                });
                ListenerRegistration childRemovedListener = firestoreQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (queryDocumentSnapshots != null && e == null){
                            for(final DocumentChange docChange: queryDocumentSnapshots.getDocumentChanges()){
                                if (docChange.getType() == DocumentChange.Type.REMOVED){
                                    childRemoved(docChange.getDocument());
                                }
                            }
                        }
                    }
                });
                ListenerRegistration childChangedListener = firestoreQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (queryDocumentSnapshots != null && e == null){
                            for(final DocumentChange docChange: queryDocumentSnapshots.getDocumentChanges()){
                                if (docChange.getType() == DocumentChange.Type.MODIFIED){
                                    childChanged(docChange.getDocument());
                                }
                            }
                        }
                    }
                });

                GeoHashQueryListener handle = new GeoHashQueryListener(childAddedListener, childRemovedListener, childChangedListener);
                handles.put(query, handle);
                addValueToReadyListener(firestoreQuery, query);
                firestoreQueries.put(query, firestoreQuery);
            }
        }
        for (Map.Entry<String, LocationInfo> info: this.locationInfos.entrySet()) {
            LocationInfo oldLocationInfo = info.getValue();

            if (oldLocationInfo != null) {
                updateLocationInfo(oldLocationInfo.documentSnapshot, oldLocationInfo.location);
            }
        }
        // remove locations that are not part of the geo query anymore
        Iterator<Map.Entry<String, LocationInfo>> it = this.locationInfos.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, LocationInfo> entry = it.next();
            if (!this.geoHashQueriesContainGeoHash(entry.getValue().geoHash)) {
                it.remove();
            }
        }

        checkAndFireReady();
    }

    private void childAdded(DocumentSnapshot documentSnapshot) {
        GeoPoint location = GeoFirestore.Companion.getLocationValue(documentSnapshot);
        if (location != null) {
            this.updateLocationInfo(documentSnapshot, location);
        }
    }

    private void childChanged(DocumentSnapshot documentSnapshot) {
        GeoPoint location = GeoFirestore.Companion.getLocationValue(documentSnapshot);
        if (location != null) {
            this.updateLocationInfo(documentSnapshot, location);
        }
    }

    private void childRemoved(DocumentSnapshot documentSnapshot) {
        final String documentID = documentSnapshot.getId();
        final LocationInfo info = this.locationInfos.get(documentID);
        if (info != null) {
            this.geoFirestore.getRefForDocumentID(documentID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()){

                        synchronized (GeoQuery.this) {
                            GeoPoint location = GeoFirestore.Companion.getLocationValue(task.getResult());
                            GeoHash hash = (location != null) ? new GeoHash(new GeoLocation(location.getLatitude(), location.getLongitude())) : null;
                            if (hash == null || !GeoQuery.this.geoHashQueriesContainGeoHash(hash)) {
                                final LocationInfo locInfo = locationInfos.remove(documentID);
                                if (locInfo != null && locInfo.inGeoQuery) {

                                    for (final GeoQueryDataEventListener listener: GeoQuery.this.eventListeners) {
                                        GeoQuery.this.geoFirestore.raiseEvent(new Runnable() {
                                            @Override
                                            public void run() {
                                                listener.onDocumentExited(locInfo.documentSnapshot);
                                            }
                                        });
                                    }

                                }
                            }
                        }

                    }
                }
            });
        }
    }

    /**
     * Adds a new GeoQueryEventListener to this GeoQuery.
     *
     * @throws IllegalArgumentException If this listener was already added
     *
     * @param listener The listener to add
     */
    public synchronized void addGeoQueryEventListener(final GeoQueryEventListener listener) {
        addGeoQueryDataEventListener(new EventListenerBridge(listener));
    }

    /**
     * Adds a new GeoQueryEventListener to this GeoQuery.
     *
     * @throws IllegalArgumentException If this listener was already added
     *
     * @param listener The listener to add
     */
    public synchronized void addGeoQueryDataEventListener(final GeoQueryDataEventListener listener) {
        if (eventListeners.contains(listener)) {
            throw new IllegalArgumentException("Added the same listener twice to a GeoQuery!");
        }
        eventListeners.add(listener);
        if (this.queries == null) {
            this.setupQueries();
        } else {
            for (final Map.Entry<String, LocationInfo> entry: this.locationInfos.entrySet()) {
                final LocationInfo info = entry.getValue();

                if (info.inGeoQuery) {
                    this.geoFirestore.raiseEvent(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDocumentEntered(info.documentSnapshot, info.location);
                        }
                    });
                }
            }
            if (this.canFireReady()) {
                this.geoFirestore.raiseEvent(new Runnable() {
                    @Override
                    public void run() {
                        listener.onGeoQueryReady();
                    }
                });
            }
        }
    }

    /**
     * Get the Firestore query(s) for this GeoQuery.
     *
     * @return The Firestore query(s) for this GeoQuery
     */
    public ArrayList<Query> getQueries() {
        Set<GeoHashQuery> oldQueries = (queries == null) ? new HashSet<GeoHashQuery>() : queries;
        Set<GeoHashQuery> newQueries = GeoHashQuery.Companion.queriesAtLocation(new GeoLocation(center.getLatitude(), center.getLongitude()), radius);
        this.queries = newQueries;

        for (GeoHashQuery query: oldQueries) {
            if (!newQueries.contains(query)) {
                GeoHashQueryListener handle = handles.get(firestoreQueries.get(query));
                if (handle != null) {
                    handle.childAddedListener.remove();
                    handle.childRemovedListener.remove();
                    handle.childChangedListener.remove();
                }
                firestoreQueries.remove(query);
                outstandingQueries.remove(query);
            }
        }

        ArrayList<Query> queries = new ArrayList<Query>();
        for (final GeoHashQuery query: newQueries) {
            if (!oldQueries.contains(query)) {
                outstandingQueries.add(query);
                CollectionReference collectionReference = this.geoFirestore.getCollectionReference();
                Query firestoreQuery = collectionReference.orderBy("g").startAt(query.getStartValue()).endAt(query.getEndValue());
                queries.add(firestoreQuery);
            }
        }
        return queries;
    }

    /**
     * Removes an event listener.
     *
     * @throws IllegalArgumentException If the listener was removed already or never added
     *
     * @param listener The listener to remove
     */
    public synchronized void removeGeoQueryEventListener(GeoQueryEventListener listener) {
        removeGeoQueryEventListener(new EventListenerBridge(listener));
    }

    /**
     * Removes an event listener.
     *
     * @throws IllegalArgumentException If the listener was removed already or never added
     *
     * @param listener The listener to remove
     */
    public synchronized void removeGeoQueryEventListener(final GeoQueryDataEventListener listener) {
        if (!eventListeners.contains(listener)) {
            throw new IllegalArgumentException("Trying to remove listener that was removed or not added!");
        }
        eventListeners.remove(listener);
        if (!this.hasListeners()) {
            reset();
        }
    }

    /**
     * Removes all event listeners from this GeoQuery.
     */
    public synchronized void removeAllListeners() {
        eventListeners.clear();
        reset();
    }

    /**
     * Returns the current center of this query.
     * @return The current center
     */
    public synchronized GeoPoint getCenter() {
        return center;
    }

    /**
     * Sets the new center of this query and triggers new events if necessary.
     * @param center The new center
     */
    public synchronized void setCenter(GeoPoint center) {
        this.center = center;
        if (this.hasListeners()) {
            this.setupQueries();
        }
    }

    /**
     * Returns the radius of the query, in kilometers.
     * @return The radius of this query, in kilometers
     */
    public synchronized double getRadius() {
        // convert from meters
        return radius / KILOMETER_TO_METER;
    }

    /**
     * Sets the radius of this query, in kilometers, and triggers new events if necessary.
     * @param radius The radius of the query, in kilometers. The maximum radius that is
     * supported is about 8587km. If a radius bigger than this is passed we'll cap it.
     */
    public synchronized void setRadius(double radius) {
        // convert to meters
        this.radius = GeoUtils.INSTANCE.capRadius(radius) * KILOMETER_TO_METER;
        if (this.hasListeners()) {
            this.setupQueries();
        }
    }

    /**
     * Sets the center and radius (in kilometers) of this query, and triggers new events if necessary.
     * @param center The new center
     * @param radius The radius of the query, in kilometers. The maximum radius that is
     * supported is about 8587km. If a radius bigger than this is passed we'll cap it.
     */
    public synchronized void setLocation(GeoPoint center, double radius) {
        this.center = center;
        // convert radius to meters
        this.radius = GeoUtils.INSTANCE.capRadius(radius) * KILOMETER_TO_METER;
        if (this.hasListeners()) {
            this.setupQueries();
        }
    }
}