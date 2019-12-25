# undertow-vh
This library is serving as a wrapper for Java's most performant webserver `undertow` to easily manage and deploy multiple virtual hosts.

## Gradle (coming soon)
```
implementation 'com.displee:undertow-vh:1.0'
```
## Usage

### Create a virtual host
```kotlin
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
```

### Start the webserver
```kotlin
val server = UndertowVH("0.0.0.0", 80, 443)
server.register(LocalHost())
server.start()
```

### Class based route
Class based routes must be placed in a package named 'route'. This 'route' package should be in the same package as the virtual host class.
```kotlin
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
```

### Pebble templating
You can easily put Java objects in the front-end using Pebble templating. Declaring Pebble variables is not necessary but it helps your IDE recognize variable types.

When transferring Java objects to the front-end you can use them like json.
```twig
{# @pebvariable name="session" type="io.undertow.server.session.Session" #}
{# @pebvariable name="request_uri" type="java.lang.String" #}
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test</title>
</head>
<body>
    You're currently on route: "{{ request_uri }}"
    <br/>
    Your session id is: {{ session.id }}
</body>
</html>
```

## Contribute
Feel free to fork and submit pull requests :)

## Credits
Original developers:

- The undertow team (http://undertow.io)
- Nick Hartskeerl
- Displee