package com.sqsong.opengllib.record

import android.os.HandlerThread
import com.sqsong.opengllib.ext.notifyAll

class EncoderHandlerThread(name: String, private val lock: Any) : HandlerThread(name) {
    var isReady: Boolean = false
        private set

    override fun onLooperPrepared() {
        synchronized(lock) {
            isReady = true
            lock.notifyAll()
        }
    }

    fun quitThreadSafely() {
        quitSafely()
        synchronized(lock) {
            isReady = false
        }
    }
}