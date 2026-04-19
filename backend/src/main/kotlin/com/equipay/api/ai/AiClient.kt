package com.equipay.api.ai

import com.equipay.api.config.OpenRouterConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory

@Serializable data class ChatMessage(val role: String, val content: String)

@Serializable data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.3,
    val max_tokens: Int = 512,
    val response_format: ResponseFormat? = null
)

@Serializable data class ResponseFormat(val type: String = "json_object")

@Serializable data class ChatCompletionResponse(
    val choices: List<Choice> = emptyList()
)

@Serializable data class Choice(val message: ChatMessage)

class AiClient(private val cfg: OpenRouterConfig) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 20_000
        }
    }

    /** Text-only completion (не ломаем существующие вызовы). */
    suspend fun chat(messages: List<ChatMessage>, forceJson: Boolean = false): String? =
        withContext(Dispatchers.IO) {
            if (cfg.apiKey.isBlank()) {
                log.warn("OpenRouter API key is blank — AI calls disabled")
                return@withContext null
            }
            try {
                val resp = client.post("${cfg.baseUrl}/chat/completions") {
                    header("Authorization", "Bearer ${cfg.apiKey}")
                    header("Content-Type", "application/json")
                    header("HTTP-Referer", "https://equipay.local")
                    header("X-Title", "EquiPay")
                    setBody(
                        ChatCompletionRequest(
                            model = cfg.model,
                            messages = messages,
                            response_format = if (forceJson) ResponseFormat() else null
                        )
                    )
                }
                if (!resp.status.isSuccess()) {
                    log.error("OpenRouter error ${resp.status}: ${resp.bodyAsText()}")
                    return@withContext null
                }
                val parsed = json.decodeFromString(ChatCompletionResponse.serializer(), resp.bodyAsText())
                parsed.choices.firstOrNull()?.message?.content
            } catch (e: Exception) {
                log.error("OpenRouter call failed", e)
                null
            }
        }

    /**
     * Vision — отправляет картинку + текстовый промпт в Gemini через OpenRouter.
     * Формат OpenAI multimodal: content = [{type:"text",...},{type:"image_url",...}]
     */
    suspend fun chatWithImage(
        systemPrompt: String,
        userText: String,
        imageBase64: String,
        mimeType: String = "image/jpeg",
        forceJson: Boolean = true
    ): String? = withContext(Dispatchers.IO) {
        if (cfg.apiKey.isBlank()) {
            log.warn("OpenRouter API key is blank — Vision disabled")
            return@withContext null
        }
        try {
            val body = buildJsonObject {
                put("model", cfg.model)
                put("temperature", 0.2)
                put("max_tokens", 512)
                if (forceJson) {
                    putJsonObject("response_format") { put("type", "json_object") }
                }
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "system")
                        put("content", systemPrompt)
                    }
                    addJsonObject {
                        put("role", "user")
                        putJsonArray("content") {
                            addJsonObject {
                                put("type", "text")
                                put("text", userText)
                            }
                            addJsonObject {
                                put("type", "image_url")
                                putJsonObject("image_url") {
                                    put("url", "data:$mimeType;base64,$imageBase64")
                                }
                            }
                        }
                    }
                }
            }

            val resp = client.post("${cfg.baseUrl}/chat/completions") {
                header("Authorization", "Bearer ${cfg.apiKey}")
                header("Content-Type", "application/json")
                header("HTTP-Referer", "https://equipay.local")
                header("X-Title", "EquiPay")
                setBody(body.toString())
            }
            if (!resp.status.isSuccess()) {
                log.error("OpenRouter vision error ${resp.status}: ${resp.bodyAsText()}")
                return@withContext null
            }
            val root = json.parseToJsonElement(resp.bodyAsText()).jsonObject
            val choices = root["choices"]?.jsonArray ?: return@withContext null
            val first = choices.firstOrNull()?.jsonObject ?: return@withContext null
            val message = first["message"]?.jsonObject ?: return@withContext null
            message["content"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            log.error("OpenRouter vision call failed", e)
            null
        }
    }
}
