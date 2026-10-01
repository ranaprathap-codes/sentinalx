package com.sentinelx.ai.service

import com.sentinelx.ai.dto.*
import com.sentinelx.shared.kernel.NotFoundException
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AiService(
    private val chatClient: ChatClient,
    @Value("\${sentinelx.ai.enabled:false}") private val aiEnabled: Boolean
) {

    fun explainFraudDecision(request: FraudExplanationRequest): FraudExplanationResponse {
        if (!aiEnabled) {
            return generateFallbackExplanation(request)
        }

        val prompt = PromptTemplate("""
            You are a fraud detection expert explaining a risk decision to a payments engineer.
            
            Transaction Context:
            - Transaction ID: {{transactionId}}
            - Risk Score: {{riskScore}}/100
            - Decision: {{decision}}
            - Triggered Rules: {{triggeredRules}}
            - Additional Context: {{context}}
            
            Provide a clear, technical explanation covering:
            1. Why this transaction received this risk score
            2. The key factors that contributed to the decision
            3. A practical recommendation for the engineer
            
            Format as JSON:
            {
              "explanation": "detailed explanation",
              "keyFactors": ["factor1", "factor2"],
              "recommendation": "actionable recommendation",
              "confidence": 0.95
            }
        """).create(request.toMap())

        try {
            val response = chatClient.prompt(prompt).call().content()
            return parseJsonResponse(response)
        } catch (e: Exception) {
            return generateFallbackExplanation(request)
        }
    }

    fun findRelatedProblems(request: RelatedProblemSearchRequest): RelatedProblemsResponse {
        if (!aiEnabled) {
            return RelatedProblemsResponse(emptyList())
        }

        val prompt = PromptTemplate("""
            You are helping an engineer find related problems in a fintech engineering community.
            
            Query: "{{query}}"
            
            Given this query, what types of problems, keywords, and patterns should they search for?
            Return a list of search strategies and related concepts as JSON.
        """).create(mapOf("query" to request.query))

        try {
            val response = chatClient.prompt(prompt).call().content()
            // In a real implementation, this would search the database
            return RelatedProblemsResponse(emptyList())
        } catch (e: Exception) {
            return RelatedProblemsResponse(emptyList())
        }
    }

    fun assistEngineering(request: EngineeringAssistantRequest): EngineeringAssistantResponse {
        if (!aiEnabled) {
            return generateFallbackAssistance(request)
        }

        val prompt = PromptTemplate("""
            You are a senior fintech engineer helping a colleague solve a technical problem.
            
            Problem: {{problemDescription}}
            Context: {{context}}
            Preferred Language: {{preferredLanguage}}
            
            Provide:
            1. 3-5 concrete suggestions/approaches
            2. 1-2 code examples in the preferred language
            3. 2-3 relevant references or patterns to research
            
            Format as JSON:
            {
              "suggestions": ["suggestion1", "suggestion2"],
              "codeExamples": [
                {"title": "Example", "language": "kotlin", "code": "...", "explanation": "..."}
              ],
              "references": ["reference1", "reference2"]
            }
        """).create(request.toMap())

        try {
            val response = chatClient.prompt(prompt).call().content()
            return parseEngineeringResponse(response)
        } catch (e: Exception) {
            return generateFallbackAssistance(request)
        }
    }

    fun analyzeIncident(request: IncidentAnalysisRequest): IncidentAnalysisResponse {
        if (!aiEnabled) {
            return generateFallbackIncidentAnalysis(request)
        }

        val prompt = PromptTemplate("""
            You are an SRE expert analyzing a production incident.
            
            Incident ID: {{incidentId}}
            Evidence: {{evidence}}
            Recent Logs: {{logs}}
            Metrics: {{metrics}}
            
            Provide:
            1. A concise summary of what happened
            2. Probable causes ranked by likelihood
            3. Recommended immediate actions
            4. Confidence level in your analysis
            
            Format as JSON:
            {
              "summary": "concise summary",
              "probableCauses": [
                {"cause": "cause description", "evidence": ["evidence1"], "likelihood": 0.8}
              ],
              "recommendedActions": ["action1", "action2"],
              "confidence": 0.85
            }
            
            IMPORTANT: Clearly distinguish between evidence (facts from logs/metrics) and hypothesis (your reasoning).
        """).create(request.toMap())

        try {
            val response = chatClient.prompt(prompt).call().content()
            return parseIncidentResponse(response)
        } catch (e: Exception) {
            return generateFallbackIncidentAnalysis(request)
        }
    }

    fun searchCommunity(request: CommunitySearchRequest): CommunitySearchResponse {
        if (!aiEnabled) {
            return CommunitySearchResponse(emptyList(), 0)
        }
        // Would integrate with actual search index
        return CommunitySearchResponse(emptyList(), 0)
    }

    private fun generateFallbackExplanation(request: FraudExplanationRequest): FraudExplanationResponse {
        val factors = request.triggeredRules.map { it["ruleName"] as String? ?: "Unknown rule" }
        val topFactor = factors.firstOrNull() ?: "No specific rules triggered"
        
        return FraudExplanationResponse(
            explanation = "This transaction was scored ${request.riskScore}/100 with a ${request.decision} decision. " +
                "The primary driver was: $topFactor. " +
                (if (factors.size > 1) " Additional factors: ${factors.drop(1).joinToString(", ")}." else ""),
            keyFactors = factors,
            recommendation = when (request.decision) {
                "BLOCK" -> "Review the triggered rules above. Consider if any rules are too aggressive for legitimate traffic patterns."
                "REVIEW" -> "Manual review recommended. Check user history and transaction context before releasing."
                else -> "Transaction approved. Monitor for any post-processing anomalies."
            },
            confidence = 0.8
        )
    }

    private fun generateFallbackAssistance(request: EngineeringAssistantRequest): EngineeringAssistantResponse {
        return EngineeringAssistantResponse(
            suggestions = listOf(
                "Break down the problem into smaller, testable components",
                "Add comprehensive logging and metrics before implementing changes",
                "Write integration tests covering the happy path and edge cases",
                "Consider feature flags for gradual rollout",
                "Document the decision rationale for future maintainers"
            ),
            codeExamples = listOf(
                CodeExample(
                    title = "Idempotent Payment Processing",
                    language = "kotlin",
                    code = """
    @Transactional
    fun processPayment(request: PaymentRequest): PaymentResponse {
        // Check idempotency
        val existing = idempotencyService.getResult(request.idempotencyKey)
        if (existing != null) return existing
        
        // Process with optimistic locking
        val payment = PaymentEntity(...)
        paymentRepository.save(payment)
        
        // Store result for idempotency
        idempotencyService.complete(request.idempotencyKey, payment.id)
        return payment.toResponse()
    }
                    """.trimIndent(),
                    explanation = "Pattern for safe retry handling in payment systems"
                )
            ),
            references = listOf(
                "Enterprise Integration Patterns - Idempotent Receiver",
                "Designing Data-Intensive Applications - Chapter 9: Consistency",
                "Spring Transaction Management Documentation"
            )
        )
    }

    private fun generateFallbackIncidentAnalysis(request: IncidentAnalysisRequest): IncidentAnalysisResponse {
        return IncidentAnalysisResponse(
            summary = "Incident analysis requires AI service. Please enable AI features or review logs manually.",
            probableCauses = listOf(
                ProbableCause(
                    cause = "Database connection pool exhaustion",
                    evidence = listOf("High latency", "Connection timeout errors"),
                    likelihood = 0.7
                ),
                ProbableCause(
                    cause = "Upstream service degradation",
                    evidence = listOf("Increased 5xx errors", "Circuit breaker open"),
                    likelihood = 0.6
                )
            ),
            recommendedActions = listOf(
                "Check database connection pool metrics",
                "Review upstream service health dashboards",
                "Examine recent deployment changes",
                "Scale horizontally if resource saturation detected"
            ),
            confidence = 0.5
        )
    }

    private fun parseJsonResponse(response: String): FraudExplanationResponse {
        // Simplified parsing - in production use Jackson
        return FraudExplanationResponse(
            explanation = response,
            keyFactors = listOf(),
            recommendation = "",
            confidence = 0.8
        )
    }

    private fun parseEngineeringResponse(response: String): EngineeringAssistantResponse {
        return EngineeringAssistantResponse(
            suggestions = listOf(),
            codeExamples = listOf(),
            references = listOf()
        )
    }

    private fun parseIncidentResponse(response: String): IncidentAnalysisResponse {
        return IncidentAnalysisResponse(
            summary = response,
            probableCauses = listOf(),
            recommendedActions = listOf(),
            confidence = 0.7
        )
    }
}

private fun FraudExplanationRequest.toMap(): Map<String, Any> {
    return mapOf(
        "transactionId" to transactionId,
        "riskScore" to riskScore,
        "decision" to decision,
        "triggeredRules" to triggeredRules,
        "context" to context
    )
}

private fun EngineeringAssistantRequest.toMap(): Map<String, Any> {
    return mapOf(
        "problemDescription" to problemDescription,
        "context" to context,
        "preferredLanguage" to preferredLanguage
    )
}

private fun IncidentAnalysisRequest.toMap(): Map<String, Any> {
    return mapOf(
        "incidentId" to incidentId,
        "evidence" to evidence,
        "logs" to logs,
        "metrics" to metrics
    )
}

private fun CommunitySearchRequest.toMap(): Map<String, Any> {
    return mapOf(
        "query" to query,
        "filters" to filters
    )
}