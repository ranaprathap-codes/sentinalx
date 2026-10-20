package com.sentinelx.ai.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sentinelx.ai.client.GeminiClient
import com.sentinelx.ai.dto.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AiService(
    private val geminiClient: GeminiClient,
    @Value("\${sentinelx.ai.enabled:true}") private val aiEnabled: Boolean
) {
    private val log = LoggerFactory.getLogger(AiService::class.java)
    private val mapper: ObjectMapper = jacksonObjectMapper()

    fun explainFraudDecision(request: FraudExplanationRequest): FraudExplanationResponse {
        if (!aiEnabled || !geminiClient.isAvailable) {
            return generateFallbackExplanation(request)
        }

        val prompt = """
            You are a fraud detection expert explaining a risk decision to a payments engineer.
            
            Transaction Context:
            - Transaction ID: ${request.transactionId}
            - Risk Score: ${request.riskScore}/100
            - Decision: ${request.decision}
            - Triggered Rules: ${request.triggeredRules}
            - Additional Context: ${request.context}
            
            Provide a clear, technical explanation covering:
            1. Why this transaction received this risk score
            2. The key factors that contributed to the decision
            3. A practical recommendation for the engineer
            
            Format strictly as JSON with this structure:
            {
              "explanation": "detailed explanation",
              "keyFactors": ["factor1", "factor2"],
              "recommendation": "actionable recommendation",
              "confidence": 0.95
            }
        """.trimIndent()

        return try {
            val response = geminiClient.generate(
                prompt = prompt,
                systemInstruction = "You are an expert fraud detection and fintech risk engineer. Output strictly valid JSON."
            )
            if (response != null) parseJsonResponse(response, request) else generateFallbackExplanation(request)
        } catch (e: Exception) {
            log.warn("Gemini fraud explanation fallback invoked: ${e.message}")
            generateFallbackExplanation(request)
        }
    }

    fun findRelatedProblems(request: RelatedProblemSearchRequest): RelatedProblemsResponse {
        if (!aiEnabled || !geminiClient.isAvailable) {
            return RelatedProblemsResponse(emptyList())
        }
        return RelatedProblemsResponse(emptyList())
    }

    fun assistEngineering(request: EngineeringAssistantRequest): EngineeringAssistantResponse {
        if (!aiEnabled || !geminiClient.isAvailable) {
            return generateFallbackAssistance(request)
        }

        val prompt = """
            You are a senior fintech engineer helping a colleague solve a technical problem.
            
            Problem: ${request.problemDescription}
            Context: ${request.context}
            Preferred Language: ${request.preferredLanguage}
            
            Provide:
            1. 3-5 concrete suggestions/approaches
            2. 1-2 code examples in the preferred language
            3. 2-3 relevant references or patterns to research
            
            Format strictly as JSON:
            {
              "suggestions": ["suggestion1", "suggestion2"],
              "codeExamples": [
                {"title": "Example", "language": "${request.preferredLanguage}", "code": "...", "explanation": "..."}
              ],
              "references": ["reference1", "reference2"]
            }
        """.trimIndent()

        return try {
            val response = geminiClient.generate(
                prompt = prompt,
                systemInstruction = "You are a principal engineer in financial systems and distributed architectures. Output valid JSON."
            )
            if (response != null) parseEngineeringResponse(response) else generateFallbackAssistance(request)
        } catch (e: Exception) {
            log.warn("Engineering assistance fallback invoked: ${e.message}")
            generateFallbackAssistance(request)
        }
    }

    fun analyzeIncident(request: IncidentAnalysisRequest): IncidentAnalysisResponse {
        if (!aiEnabled || !geminiClient.isAvailable) {
            return generateFallbackIncidentAnalysis(request)
        }

        val prompt = """
            You are an SRE expert analyzing a production incident.
            
            Incident ID: ${request.incidentId}
            Evidence: ${request.evidence}
            Recent Logs: ${request.logs}
            Metrics: ${request.metrics}
            
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
        """.trimIndent()

        return try {
            val response = geminiClient.generate(
                prompt = prompt,
                systemInstruction = "You are a Staff SRE and incident commander. Clearly separate observed evidence from hypotheses. Output valid JSON."
            )
            if (response != null) parseIncidentResponse(response) else generateFallbackIncidentAnalysis(request)
        } catch (e: Exception) {
            log.warn("Incident analysis fallback invoked: ${e.message}")
            generateFallbackIncidentAnalysis(request)
        }
    }

    fun searchCommunity(request: CommunitySearchRequest): CommunitySearchResponse {
        return CommunitySearchResponse(emptyList(), 0)
    }

    /**
     * Freeform streaming assistant for general fintech and security Q&A.
     */
    fun answerQuestion(question: String): Sequence<String> {
        if (!geminiClient.isAvailable) {
            return sequenceOf("SentinelX AI is in offline fallback mode. Please configure GEMINI_API_KEY to activate live AI answers.")
        }
        return geminiClient.streamGenerate(
            prompt = question,
            systemInstruction = "You are SentinelX AI, an intelligent cybersecurity and fintech copilot. Provide actionable, concise, and technically accurate responses."
        )
    }

    /**
     * Concise 2-3 sentence risk explanation for UI callouts.
     */
    fun explainRisk(score: Int, decision: String, rules: List<String>): String {
        if (!geminiClient.isAvailable) {
            val ruleStr = if (rules.isNotEmpty()) rules.joinToString(", ") else "No specific rules"
            return "Transaction evaluated with score $score/100 resulted in $decision decision based on triggered rules: $ruleStr."
        }
        val prompt = "Score: $score/100, Decision: $decision, Rules triggered: ${rules.joinToString(", ")}. Explain why in 2 concise sentences."
        return geminiClient.generate(prompt, "You are an automated fraud detection analyst. Be concise and direct.")
            ?: "Transaction evaluated with score $score/100 resulted in $decision decision."
    }

    private fun generateFallbackExplanation(request: FraudExplanationRequest): FraudExplanationResponse {
        val factors = request.triggeredRules.mapNotNull { it["ruleName"] as? String }
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
                            val existing = idempotencyService.getResult(request.idempotencyKey)
                            if (existing != null) return existing
                            val payment = PaymentEntity(...)
                            paymentRepository.save(payment)
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
            summary = "Automated preliminary analysis: potential latency or downstream anomaly detected.",
            probableCauses = listOf(
                ProbableCause(
                    cause = "Database connection pool exhaustion or lock contention",
                    evidence = listOf("Elevated latency", "Connection pool metric spikes"),
                    likelihood = 0.7
                ),
                ProbableCause(
                    cause = "Upstream service degradation or timeout",
                    evidence = listOf("Non-2xx response codes", "Circuit breaker triggers"),
                    likelihood = 0.6
                )
            ),
            recommendedActions = listOf(
                "Check database active connection metrics and slow queries",
                "Review circuit breaker status and downstream endpoints",
                "Verify recent deployment or configuration rollout changes"
            ),
            confidence = 0.6
        )
    }

    private fun parseJsonResponse(response: String, fallbackRequest: FraudExplanationRequest): FraudExplanationResponse {
        return try {
            val cleanJson = cleanJsonOutput(response)
            mapper.readValue(cleanJson, FraudExplanationResponse::class.java)
        } catch (e: Exception) {
            FraudExplanationResponse(
                explanation = response.replace(Regex("```json|```"), "").trim(),
                keyFactors = fallbackRequest.triggeredRules.mapNotNull { it["ruleName"] as? String },
                recommendation = "Review transaction metrics against established risk profile.",
                confidence = 0.85
            )
        }
    }

    private fun parseEngineeringResponse(response: String): EngineeringAssistantResponse {
        return try {
            val cleanJson = cleanJsonOutput(response)
            mapper.readValue(cleanJson, EngineeringAssistantResponse::class.java)
        } catch (e: Exception) {
            generateFallbackAssistance(EngineeringAssistantRequest(problemDescription = "Parsed assistance"))
        }
    }

    private fun parseIncidentResponse(response: String): IncidentAnalysisResponse {
        return try {
            val cleanJson = cleanJsonOutput(response)
            mapper.readValue(cleanJson, IncidentAnalysisResponse::class.java)
        } catch (e: Exception) {
            generateFallbackIncidentAnalysis(IncidentAnalysisRequest(incidentId = java.util.UUID.randomUUID(), evidence = emptyMap()))
        }
    }

    private fun cleanJsonOutput(raw: String): String {
        return raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}