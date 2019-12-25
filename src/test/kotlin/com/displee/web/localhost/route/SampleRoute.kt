package com.displee.web.localhost.route

import com.displee.undertow.host.route.*
import com.google.gson.JsonObject
import io.undertow.server.HttpServerExchange

@RouteManifest("/register", GET)
class SampleRoute : RouteHandler() {

    override fun model(exchange: HttpServerExchange) {
        super.model(exchange)
        //feel free to pass any parameters to the front end!
        model["test"] = "pebble works!"
    }

    override fun handleRequest(exchange: HttpServerExchange) {
        //check if we've received the required for this route query params
        if (!exchange.checkQueryParameters("username", "first_name", "last_name")) {
            exchange.responseSender.send("Error received not enough parameters.")
            return
        }
        val queryParameters = exchange.getQueryParametersAsMap()
        val username = queryParameters["username"]
        val firstName = queryParameters["first_name"]
        val lastName = queryParameters["last_name"]
        //we can do any logic here, for example put the data above in a database
        val json = JsonObject()
        json.addProperty("error", -1)
        json.addProperty("message", "Successfully registered!")
        exchange.send(json)
    }

}