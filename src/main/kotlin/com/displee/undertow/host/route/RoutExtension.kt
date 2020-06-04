package com.displee.undertow.host.route

import com.displee.undertow.util.CHARSET
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

private val logger = mu.KotlinLogging.logger {}

fun HttpServerExchange.redirect(url: String) {
    statusCode = StatusCodes.FOUND
    responseHeaders.put(Headers.LOCATION, url)
    endExchange()
}

fun HttpServerExchange.getSession(): Session {
    val sessionManager = getAttachment(SessionManager.ATTACHMENT_KEY)
    val sessionConfig = getAttachment(SessionConfig.ATTACHMENT_KEY)
    var session = sessionManager.getSession(this, sessionConfig)
    if (session == null) {
        session = sessionManager.createSession(this, sessionConfig)
    }
    return session
}

fun HttpServerExchange.parseFormDataAsJson(): JsonObject {
    val json = JsonObject()
    loopThroughFormData(this) { formData ->
        for (key in formData) {
            val deque = formData.get(key)
            val item = deque.first
            if (item.isFileItem) {
                continue
            }
            json.addProperty(key, item.value)
        }
    }
    return json
}

fun HttpServerExchange.getQueryParametersAsJson(): JsonObject {
    val json = JsonObject()
    for(i in queryParameters) {
        json.addProperty(i.key, i.value.first)
    }
    return json
}

fun HttpServerExchange.parseFormDataAsMap(): Map<String, String> {
    val map = HashMap<String, String>()
    loopThroughFormData(this) { formData ->
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

fun HttpServerExchange.getQueryParametersAsMap(): Map<String, String> {
    val map = HashMap<String, String>()
    for(i in queryParameters) {
        map[i.key] = i.value.first
    }
    return map
}

fun HttpServerExchange.parseAllFormData(): Map<String, Array<String>> {
    val map = HashMap<String, Array<String>>()
    loopThroughFormData(this) { formData ->
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

fun HttpServerExchange.parseFileFormData(): Map<String, File> {
    val map = HashMap<String, File>()
    loopThroughFormData(this) { formData ->
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

fun HttpServerExchange.parseAllFileFormData(): Map<String, Array<File>> {
    val map = HashMap<String, Array<File>>()
    loopThroughFormData(this) { formData ->
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

private fun loopThroughFormData(exchange: HttpServerExchange, unit: (fromData: FormData) -> Unit) {
    try {
        val factory = FormParserFactory.builder().withDefaultCharset(CHARSET.name()).build()
        val formDataParser = factory.createParser(exchange) ?: return
        exchange.startBlocking()
        val formData = formDataParser.parseBlocking()
        unit.apply { formData }
        exchange.startBlocking(null)
    } catch(t: Throwable) {
        logger.error("", t)
    }
}

fun HttpServerExchange.send(string: String) {
    responseHeaders.put(Headers.CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString())
    responseSender.send(string)
}

fun HttpServerExchange.send(json: JsonElement) {
    responseHeaders.put(Headers.CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
    responseSender.send(json.toString())
}

fun Map<String, String>.containsNullOrBlank(vararg params: String): Boolean {
    for(param in params) {
        if (this[param].isNullOrBlank()) {
            return true
        }
    }
    return false
}

fun Map<String, String>.containsNotNullOrBlank(vararg params: String): Boolean {
    for(param in params) {
        if (!this[param].isNullOrBlank()) {
            return true
        }
    }
    return false
}

fun JsonObject.containsNullOrBlank(vararg params: String): Boolean {
    for(param in params) {
        val value = get(param)
        if (value == null || (value.isJsonPrimitive && value.asString.isNullOrBlank())) {
            return true
        }
    }
    return false
}

fun JsonObject.containsNotNullOrBlank(vararg params: String): Boolean {
    for(param in params) {
        val value = get(param)
        if (value != null && !(value.isJsonPrimitive && value.asString.isNullOrBlank())) {
            return true
        }
    }
    return false
}