package com.sqsong.opengllib.ext

fun Any.wait() {
    (this as Object).wait()
}

fun Any.notify() {
    (this as Object).notify()
}

fun Any.notifyAll() {
    (this as Object).notifyAll()
}
