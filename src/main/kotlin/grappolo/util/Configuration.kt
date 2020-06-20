package grappolo.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import grappolo.Similarity
import java.net.URL

class Configuration(val minSimilarity: Similarity = 0.0,
                    val linesToSkip: Int = 0, linesToRead: Int = Int.MAX_VALUE) {

    companion object {

        val default by lazy { load() }

        fun load() = load("configuration.yml")

        fun load(resourceName: String): Configuration =
                load(Thread.currentThread().contextClassLoader.getResource(resourceName)!!)

        fun load(url: URL): Configuration {
            val mapper = ObjectMapper(YAMLFactory())
            mapper.registerModule(KotlinModule())
            return mapper.readValue(url.openStream(), Configuration::class.java)
        }
    }
}
