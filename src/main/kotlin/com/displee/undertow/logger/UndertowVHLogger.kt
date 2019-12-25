package com.displee.undertow.logger

import java.util.logging.Level
import java.util.logging.Logger

class UndertowVHLogger {

    companion object {

        public fun log(message: String) {
            println(message)
        }

        public fun warn(message: String) {
            log(message)
        }

        public fun error(message: String) {
            System.err.println(message)
        }

        public fun disableXnioLogger() {
            Logger.getLogger("org.xnio").level = Level.OFF
        }

    }

}