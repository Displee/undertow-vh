package com.displee.undertow.template

import java.nio.file.Path

interface TemplateProcessor {
    fun render(path: Path, model: Map<String, Any>): String
    fun contentType(): String
}