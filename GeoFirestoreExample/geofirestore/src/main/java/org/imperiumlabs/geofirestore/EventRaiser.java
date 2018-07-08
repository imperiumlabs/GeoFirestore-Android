package org.imperiumlabs.geofirestore;

interface EventRaiser {
    void raiseEvent(Runnable r);
}
