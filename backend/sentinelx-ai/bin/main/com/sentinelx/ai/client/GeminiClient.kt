package com.sentinelx.ai.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.IOException
import java.time.Duration

/**
 * Lightweight production HTTP client for Google Gemini API.
 * Uses gemini-3.8-flash by default.
 */
@Component
class GeminiClient(
    @Value("\${sentinelx.ai.gemini.api-key:}")
    private val apiKey: String,

    @Value("\${sentinelx.ai.gemini.model:gemini-3.8-flash}")
    private val model: String
) {
    private val log = LoggerFactory.getLogger(GeminiClient::class.java)
    private val http = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(15))
        .readTimeout(Duration.ofSeconds(60))
        .build()
    private val mapper: ObjectMapper = jacksonObjectMapper()

    val isAvailable: Boolean get() = apiKey.isNotBlank()

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models"

    /**
     * Generate content synchronously from Gemini.
     */
    fun generate(prompt: String, systemInstruction: String? = null): String? {
        if (!isAvailable) {
            log.info("Gemini API key not configured — fallback mode active")
            return null
        }

        val contents = listOf(
            mapOf(
                "role" to "user",
                "parts" to listOf(mapOf("text" to prompt))
            )
        )

        val requestBody = mutableMapOf<String, Any>("contents" to contents)
        if (!systemInstruction.isNullOrBlank()) {
            requestBody["system_instruction"] = mapOf(
                "parts" to listOf(mapOf("text" to systemInstruction))
            )
        }

        val json = mapper.writeValueAsString(requestBody)
        val request = Request.Builder()
            .url("$baseUrl/$model:generateContent?key=$apiKey")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string()
                    log.error("Gemini API error (HTTP ${response.code}): $err")
                    return null
                }
                val body = mapper.readTree(response.body?.string())
                body["candidates"]?.get(0)?.get("content")?.get("parts")?.get(0)?.get("text")?.asText()
            }
        } catch (e: Exception) {
            log.error("Gemini API call failed: ${e.message}", e)
            null
        }
    }

    /**
     * Stream content tokens as SSE sequence from Gemini.
     */
    fun streamGenerate(prompt: String, systemInstruction: String? = null): Sequence<String> {
        if (!isAvailable) return emptySequence()

        val contents = listOf(
            mapOf(
                "role" to "user",
                "parts" to listOf(mapOf("text" to prompt))
            )
        )

        val requestBody = mutableMapOf<String, Any>("contents" to contents)
        if (!systemInstruction.isNullOrBlank()) {
            requestBody["system_instruction"] = mapOf(
                "parts" to listOf(mapOf("text" to systemInstruction))
            )
        }

        val json = mapper.writeValueAsString(requestBody)
        val request = Request.Builder()
            .url("$baseUrl/$model:streamGenerateContent?alt=sse&key=$apiKey")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        return sequence {
            try {
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@sequence
                    response.body?.source()?.use { source ->
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (line.startsWith("data: ")) {
                                val data = line.removePrefix("data: ").trim()
                                if (data == "[DONE]") break
                                var text: String? = null
                                try {
                                    val node = mapper.readTree(data)
                                    text = node["candidates"]?.get(0)?.get("content")
                                        ?.get("parts")?.get(0)?.get("text")?.asText()
                                } catch (_: Exception) {
                                    // skip non-JSON or partial chunk
                                }
                                if (!text.isNullOrBlank()) {
                                    yield(text)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                log.error("Gemini stream failed: ${e.message}")
            }
        }
    }
}

