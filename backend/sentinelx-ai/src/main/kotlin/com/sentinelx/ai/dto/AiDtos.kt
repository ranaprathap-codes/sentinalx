package com.sentinelx.ai.dto

import jakarta.validation.constraints.*
import java.util.UUID

data class FraudExplanationRequest(
    @NotNull val transactionId: UUID,
    val riskScore: Int,
    val decision: String,
    val triggeredRules: List<Map<String, Any>>,
    val context: Map<String, Any> = emptyMap()
)

data class FraudExplanationResponse(
    val explanation: String,
    val keyFactors: List<String>,
    val recommendation: String,
    val confidence: Double
)

data class RelatedProblemSearchRequest(
    @NotBlank val query: String,
    val maxResults: Int = 10,
    val includeResolved: Boolean = false
)

data class RelatedProblemResponse(
    val reportId: UUID,
    val title: String,
    val type: String,
    val severity: String?,
    val status: String,
    val similarity: Double,
    val matchedReason: String
)

data class RelatedProblemsResponse(
    val results: List<RelatedProblemResponse>
)

data class EngineeringAssistantRequest(
    @NotBlank val problemDescription: String,
    val context: Map<String, Any> = emptyMap(),
    val preferredLanguage: String = "kotlin"
)

data class EngineeringAssistantResponse(
    val suggestions: List<String>,
    val codeExamples: List<CodeExample>,
    val references: List<String>
)

data class CodeExample(
    val title: String,
    val language: String,
    val code: String,
    val explanation: String
)

data class IncidentAnalysisRequest(
    val incidentId: UUID,
    val evidence: Map<String, Any>,
    val logs: List<String> = emptyList(),
    val metrics: Map<String, Double> = emptyMap()
)

data class IncidentAnalysisResponse(
    val summary: String,
    val probableCauses: List<ProbableCause>,
    val recommendedActions: List<String>,
    val confidence: Double
)

data class ProbableCause(
    val cause: String,
    val evidence: List<String>,
    val likelihood: Double
)

data class CommunitySearchRequest(
    @NotBlank val query: String,
    val filters: SearchFilters = SearchFilters()
)

data class SearchFilters(
    val types: List<String> = emptyList(),
    val statuses: List<String> = emptyList(),
    val components: List<String> = emptyList(),
    val dateFrom: String? = null,
    val dateTo: String? = null
)

data class CommunitySearchResponse(
    val results: List<SearchResult>,
    val totalHits: Long
)

data class SearchResult(
    val id: UUID,
    val type: String,
    val title: String,
    val snippet: String,
    val highlights: List<String>,
    val score: Double
)