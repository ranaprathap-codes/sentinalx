package com.sentinelx.ai.controller

import com.sentinelx.ai.dto.*
import com.sentinelx.ai.service.AiService
import com.sentinelx.shared.kernel.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.util.UUID

data class AskRequest(@field:NotBlank val question: String)
data class RiskExplainResponse(val explanation: String)

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Features", description = "AI-powered explanations, search, assistance, and live streaming copilot")
class AiController(
    private val aiService: AiService
) {

    @PostMapping("/explain/fraud")
    @Operation(summary = "Get AI explanation for fraud decision")
    fun explainFraud(@Valid @RequestBody request: FraudExplanationRequest): ResponseEntity<ApiResponse<FraudExplanationResponse>> {
        val response = aiService.explainFraudDecision(request)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @PostMapping("/search/problems")
    @Operation(summary = "Find related engineering problems")
    fun findRelatedProblems(@Valid @RequestBody request: RelatedProblemSearchRequest): ResponseEntity<ApiResponse<RelatedProblemsResponse>> {
        val response = aiService.findRelatedProblems(request)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @PostMapping("/assist/engineering")
    @Operation(summary = "Get engineering assistance for a problem")
    fun assistEngineering(@Valid @RequestBody request: EngineeringAssistantRequest): ResponseEntity<ApiResponse<EngineeringAssistantResponse>> {
        val response = aiService.assistEngineering(request)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @PostMapping("/analyze/incident")
    @Operation(summary = "Analyze an incident with AI")
    fun analyzeIncident(@Valid @RequestBody request: IncidentAnalysisRequest): ResponseEntity<ApiResponse<IncidentAnalysisResponse>> {
        val response = aiService.analyzeIncident(request)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @PostMapping("/search/community")
    @Operation(summary = "Search community content with AI")
    fun searchCommunity(@Valid @RequestBody request: CommunitySearchRequest): ResponseEntity<ApiResponse<CommunitySearchResponse>> {
        val response = aiService.searchCommunity(request)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @PostMapping("/ask", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @Operation(summary = "Stream interactive response from Gemini copilot")
    fun ask(@Valid @RequestBody request: AskRequest): ResponseEntity<StreamingResponseBody> {
        val body = StreamingResponseBody { outputStream ->
            val chunks = aiService.answerQuestion(request.question)
            for (chunk in chunks) {
                val sseData = "data: $chunk\n\n"
                outputStream.write(sseData.toByteArray())
                outputStream.flush()
            }
        }
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(body)
    }

    @GetMapping("/explain/risk")
    @Operation(summary = "Quick AI risk explanation for score and rules")
    fun explainRisk(
        @RequestParam score: Int,
        @RequestParam decision: String,
        @RequestParam(required = false, defaultValue = "") rules: List<String>
    ): ResponseEntity<ApiResponse<RiskExplainResponse>> {
        val text = aiService.explainRisk(score, decision, rules)
        return ResponseEntity.ok(ApiResponse.ok(RiskExplainResponse(text)))
    }
}