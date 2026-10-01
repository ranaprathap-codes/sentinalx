package com.sentinelx.ai.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiConfig {

    @Bean
    fun chatClient(
        @Value("\${sentinelx.ai.provider:ollama}") provider: String,
        @Value("\${sentinelx.ai.ollama.base-url:http://localhost:11434}") ollamaBaseUrl: String,
        @Value("\${sentinelx.ai.ollama.model:llama3.1}") ollamaModel: String,
        @Value("\${sentinelx.ai.openai.api-key:}") openAiApiKey: String,
        @Value("\${sentinelx.ai.openai.model:gpt-4o-mini}") openAiModel: String
    ): ChatClient {
        val chatModel = when (provider.lowercase()) {
            "ollama" -> OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .defaultOptions(OllamaChatModel.defaultOptions().withModel(ollamaModel))
                .build()
            "openai" -> {
                if (openAiApiKey.isBlank()) {
                    throw IllegalStateException("OpenAI API key required when provider is openai")
                }
                OpenAiChatModel.builder()
                    .apiKey(openAiApiKey)
                    .defaultOptions(OpenAiChatOptions.builder().model(openAiModel).build())
                    .build()
            }
            else -> throw IllegalArgumentException("Unknown AI provider: $provider")
        }

        return ChatClient.builder(chatModel).build()
    }
}