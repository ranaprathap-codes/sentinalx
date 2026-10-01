package com.sentinelx.risk.controller

import com.sentinelx.risk.dto.EvaluateRiskRequest
import com.sentinelx.risk.dto.RiskEvaluationResponse
import com.sentinelx.risk.dto.RiskRuleRequest
import com.sentinelx.risk.dto.RiskRuleResponse
import com.sentinelx.risk.service.RiskService
import com.sentinelx.shared.kernel.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/risk")
@Tag(name = "Risk Engine", description = "Fraud detection and risk evaluation")
class RiskController(
    private val riskService: RiskService
) {

    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate transaction risk")
    fun evaluateRisk(@Valid @RequestBody request: EvaluateRiskRequest): ResponseEntity<ApiResponse<RiskEvaluationResponse>> {
        val response = riskService.evaluateRisk(request)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/evaluation/{transactionId}")
    @Operation(summary = "Get risk evaluation for transaction")
    fun getEvaluation(@PathVariable transactionId: UUID): ResponseEntity<ApiResponse<RiskEvaluationResponse>> {
        val response = riskService.getEvaluation(transactionId)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    @Operation(summary = "Create a new risk rule")
    fun createRule(@Valid @RequestBody request: RiskRuleRequest): ResponseEntity<ApiResponse<RiskRuleResponse>> {
        val response = riskService.createRule(request)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @PutMapping("/rules/{ruleId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    @Operation(summary = "Update a risk rule")
    fun updateRule(@PathVariable ruleId: UUID, @Valid @RequestBody request: RiskRuleRequest): ResponseEntity<ApiResponse<RiskRuleResponse>> {
        val response = riskService.updateRule(ruleId, request)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/rules/{ruleId}")
    @Operation(summary = "Get a risk rule by ID")
    fun getRule(@PathVariable ruleId: UUID): ResponseEntity<ApiResponse<RiskRuleResponse>> {
        val response = riskService.getRule(ruleId)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/rules")
    @Operation(summary = "List all risk rules")
    fun listRules(): ResponseEntity<ApiResponse<List<RiskRuleResponse>>> {
        val response = riskService.listRules()
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @DeleteMapping("/rules/{ruleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a risk rule")
    fun deleteRule(@PathVariable ruleId: UUID): ResponseEntity<ApiResponse<Unit>> {
        riskService.deleteRule(ruleId)
        return ResponseEntity.ok(ApiResponse.ok(Unit))
    }

    @PostMapping("/rules/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Initialize default risk rules")
    fun initializeDefaultRules(): ResponseEntity<ApiResponse<Unit>> {
        riskService.initializeDefaultRules()
        return ResponseEntity.ok(ApiResponse.ok(Unit))
    }
}