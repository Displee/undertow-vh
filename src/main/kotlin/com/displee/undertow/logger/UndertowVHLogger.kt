package com.displee.undertow.logger

import java.lang.Exception
import java.util.logging.Level
import java.util.logging.Logger

fun log(message: String) {
    println(message)
}

fun log(exception: Exception) {
    exception.printStackTrace()
}

fun warn(message: String) {
    log(message)
}

fun err(message: String) {
    System.err.println(message)
}

fun disableXnioLogger() {
    Logger.getLogger("org.xnio").level = Level.OFF
}