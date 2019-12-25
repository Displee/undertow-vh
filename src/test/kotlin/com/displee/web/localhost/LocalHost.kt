package com.displee.web.localhost

import com.displee.undertow.host.VirtualHost
import com.displee.undertow.host.route.send
import com.google.gson.JsonObject

class LocalHost : VirtualHost("localhost") {

    override fun routes() {
        super.routes() //for class based routes
        get("/", publicHtml().resolve("index.html"))
        get("/test") {
            val json = JsonObject()
            json.addProperty("message", "Hello")
            it.send(json)
        }
        get("/pebble", privateHtml().resolve("test.peb"))
    }

}