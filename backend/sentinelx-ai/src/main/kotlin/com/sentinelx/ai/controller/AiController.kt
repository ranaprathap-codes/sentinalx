package com.sentinelx.ai.controller

import com.sentinelx.ai.dto.*
import com.sentinelx.ai.service.AiService
import com.sentinelx.shared.kernel.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Features", description = "AI-powered explanations, search, and assistance")
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
}