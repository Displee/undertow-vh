package com.displee.undertow.host.route

import com.google.common.base.Charsets
import com.google.common.net.MediaType
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.form.FormData
import io.undertow.server.handlers.form.FormParserFactory
import io.undertow.server.session.Session
import io.undertow.server.session.SessionConfig
import io.undertow.server.session.SessionManager
import io.undertow.util.Headers
import io.undertow.util.StatusCodes
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.set

public fun HttpServerExchange.redirect(url: String) {
    statusCode = StatusCodes.FOUND
    responseHeaders.put(Headers.LOCATION, url)
    endExchange()
}

public fun HttpServerExchange.getSession(): Session {
    val sessionManager = getAttachment(SessionManager.ATTACHMENT_KEY);
    val sessionConfig = getAttachment(SessionConfig.ATTACHMENT_KEY)
    var session = sessionManager.getSession(this, sessionConfig)
    if (session == null) {
        session = sessionManager.createSession(this, sessionConfig)
    }
    return session
}

public fun HttpServerExchange.check(vararg params: String): Boolean {
    return RoutExtension.check(querySingleStringParameters(), *params)
}

public fun HttpServerExchange.queryAsJson(): JsonObject {
    val json = JsonObject()
    loopThroughParameters(this) { formData ->
        for (key in formData) {
            val deque = formData.get(key)
            if (deque.size != 1) {
                continue
            }
            val item = deque.first
            if (item.isFileItem) {
                continue
            }
            json.addProperty(key, item.value)
        }
    }
    return json
}

public fun HttpServerExchange.querySingleStringParameters(): Map<String, String> {
    val map = HashMap<String, String>()
    loopThroughParameters(this) { formData ->
        for (key in formData) {
            val deque = formData.get(key)
            if (deque.size != 1) {
                continue
            }
            val item = deque.first
            if (item.isFileItem) {
                continue
            }
            map[key] = item.value
        }
    }
    return map
}

public fun HttpServerExchange.queryAllStringParameters(): Map<String, Array<String>> {
    val map = HashMap<String, Array<String>>()
    loopThroughParameters(this) { formData ->
        for (key in formData) {
            val deque = formData.get(key)
            val list = ArrayList<String>(deque.size)
            for(formValue in deque) {
                if (formValue.isFileItem) {
                    continue
                }
                list.add(formValue.value)
            }
            map[key] = list.toArray(arrayOfNulls(0))
        }
    }
    return map
}

public fun HttpServerExchange.querySingleFileParameters(): Map<String, File> {
    val map = HashMap<String, File>()
    loopThroughParameters(this) { formData ->
        for (key in formData) {
            val deque = formData.get(key)
            if (deque.size != 1) {
                continue
            }
            val firstItem = deque.first
            if (!firstItem.isFileItem) {
                continue
            }
            map[key] = firstItem.fileItem.file.toFile()
        }
    }
    return map
}

public fun HttpServerExchange.queryAllFileParameters(): Map<String, Array<File>> {
    val map = HashMap<String, Array<File>>()
    loopThroughParameters(this) { formData ->
        for (key in formData) {
            val deque = formData.get(key)
            val list = ArrayList<File>(deque.size)
            for(formValue in deque) {
                if (!formValue.isFileItem) {
                    continue
                }
                list.add(formValue.fileItem.file.toFile())
            }
            map[key] = list.toArray(arrayOfNulls(0))
        }
    }
    return map
}

private fun loopThroughParameters(exchange: HttpServerExchange, unit: (fromData: FormData) -> Unit) {
    try {
        val builder = FormParserFactory.builder()
        builder.defaultCharset = Charsets.UTF_8.name()
        val formDataParser = builder.build().createParser(exchange) ?: return
        exchange.startBlocking()
        val formData = formDataParser.parseBlocking()
        unit.apply { formData }
        exchange.startBlocking(null)
    } catch(e: Exception) {
        e.printStackTrace()
    }
}

public fun HttpServerExchange.send(json: JsonElement) {
    responseHeaders.put(Headers.CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
    responseSender.send(json.toString())
}

public class RoutExtension {

    companion object {
        public fun check(form: JsonObject, vararg params: String): Boolean {
            for(param in params) {
                if (form.get(param) == null) {
                    return false
                }
            }
            return true
        }

        public fun check(form: Map<String, String>, vararg params: String): Boolean {
            for(param in params) {
                if (form[param] == null) {
                    return false
                }
            }
            return true
        }
    }

}