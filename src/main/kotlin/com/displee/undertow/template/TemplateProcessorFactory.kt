package com.displee.undertow.template

import com.displee.undertow.template.impl.PebbleTemplateProcessor

class TemplateProcessorFactory {

    companion object {

        private val templates = HashMap<String, TemplateProcessor>()

        init {
            register(PebbleTemplateProcessor())
        }

        public fun register(templateProcessor: TemplateProcessor) {
            val manifest = templateProcessor.javaClass.getAnnotation(TemplateProcessorManifest::class.java)
            checkNotNull(manifest) { "No manifest found for ${templateProcessor.javaClass.simpleName}." }
            for(extension in manifest.extensions) {
                templates[extension] = templateProcessor
            }
        }

        public fun get(extension: String): TemplateProcessor? {
            return templates[extension]
        }

    }

}