package org.imperiumlabs.geofirestore;

import com.google.firebase.firestore.*;
import java.lang.Exception;

// FULLY TESTED

/**
 * GeoQuery notifies listeners with this interface about documents that entered, exited, or moved within the query.
 */
final class EventListenerBridge implements GeoQueryDataEventListener {
    private final GeoQueryEventListener listener;

    public EventListenerBridge(final GeoQueryEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void onDocumentEntered(final DocumentSnapshot documentSnapshot, final GeoPoint location) {
        listener.onKeyEntered(documentSnapshot.getId(), location);
    }

    @Override
    public void onDocumentExited(final DocumentSnapshot documentSnapshot) {
        listener.onKeyExited(documentSnapshot.getId());
    }

    @Override
    public void onDocumentMoved(final DocumentSnapshot documentSnapshot, final GeoPoint location) {
        listener.onKeyMoved(documentSnapshot.getId(), location);
    }

    @Override
    public void onDocumentChanged(final DocumentSnapshot documentSnapshot, final GeoPoint location) {
        // No-op.
    }

    @Override
    public void onGeoQueryReady() {
        listener.onGeoQueryReady();
    }

    @Override
    public void onGeoQueryError(final Exception exception) {
        listener.onGeoQueryError(exception);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final EventListenerBridge that = (EventListenerBridge) o;
        return listener.equals(that.listener);
    }

    @Override
    public int hashCode() {
        return listener.hashCode();
    }
}
