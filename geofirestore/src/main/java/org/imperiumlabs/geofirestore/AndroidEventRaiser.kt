package org.imperiumlabs.geofirestore

import android.os.Handler
import android.os.Looper

// FULLY TESTED

class AndroidEventRaiser: EventRaiser {
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    override fun raiseEvent(r: Runnable) {
        this.mainThreadHandler.post(r)
    }
}
