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
        get("/pebble", privateHtml().resolve("test.peb"), mapOf(Pair("message", "Undertow is awesome!")))
        get("/jetbrick", privateHtml().resolve("test.jetx"), mapOf(Pair("message", "Undertow is awesome!")))
        get("/freemarker", privateHtml().resolve("test.ftl"), mapOf(Pair("message", "Undertow is awesome!")))
        get("/velocity", privateHtml().resolve("test.vm"), mapOf(Pair("message", "Undertow is awesome!")))
        get("/thymeleaf", privateHtml().resolve("test.tl"), mapOf(Pair("message", "Undertow is awesome!")))
        get("/jade4j", privateHtml().resolve("test.jade"), mapOf(Pair("message", "Undertow is awesome!")))
        get("/groovy", privateHtml().resolve("test.groovy"), mapOf(Pair("message", "Undertow is awesome!")))
    }

}