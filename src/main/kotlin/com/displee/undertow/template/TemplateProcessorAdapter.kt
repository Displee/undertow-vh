package com.displee.undertow.template

import org.reflections.Reflections

object TemplateProcessorAdapter {

    private val templateProcessors = HashMap<String, TemplateProcessor>()

    init {
        Reflections(javaClass.`package`.name + ".impl").getSubTypesOf(TemplateProcessor::class.java).forEach { register(it.newInstance()) }
    }

    fun register(templateProcessor: TemplateProcessor) {
        val manifest = templateProcessor.javaClass.getAnnotation(TemplateProcessorManifest::class.java)
        checkNotNull(manifest) { "No manifest found for ${templateProcessor.javaClass.simpleName}." }
        for(extension in manifest.extensions) {
            templateProcessors[extension] = templateProcessor
        }
    }

    fun get(extension: String): TemplateProcessor? {
        return templateProcessors[extension]
    }

}