package com.displee.web.localhost

import com.displee.undertow.host.VirtualHost
import com.displee.undertow.host.route.send
import com.google.gson.JsonObject

class LocalHost : VirtualHost("localhost") {

    override fun routes() {
        super.routes()
        get("/test", privateHtml().resolve("test.peb"))
        get("/lol") {
            val json = JsonObject()
            json.addProperty("message", "Hello")
            it.send(json)
        }
    }

}