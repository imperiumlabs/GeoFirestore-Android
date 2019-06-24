package org.imperiumlabs.geofirestore

// FULLY TESTED

interface EventRaiser {
    fun raiseEvent(r: Runnable)
}
