package org.imperiumlabs.geofirestore

import java.util.concurrent.Executors

// FULLY TESTED

class ThreadEventRaiser: EventRaiser {

    private val executorService = Executors.newSingleThreadExecutor()

    override fun raiseEvent(r: Runnable) {
        this.executorService.submit(r)
    }
}
