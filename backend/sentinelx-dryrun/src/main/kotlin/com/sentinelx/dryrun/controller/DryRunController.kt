package com.sentinelx.dryrun.controller

import com.sentinelx.auth.domain.User
import com.sentinelx.dryrun.dto.CreateDryRunRequest
import com.sentinelx.dryrun.dto.DryRunListResponse
import com.sentinelx.dryrun.dto.DryRunResponse
import com.sentinelx.dryrun.service.DryRunService
import com.sentinelx.shared.kernel.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/dryrun")
@Tag(name = "Dry Run", description = "Test proposed risk rule changes against historical transactions")
class DryRunController(
    private val dryRunService: DryRunService
) {

    @PostMapping
    @Operation(summary = "Create a new dry run")
    fun createDryRun(
        @Valid @RequestBody request: CreateDryRunRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<DryRunResponse>> {
        val user = getCurrentUser(authentication)
        val response = dryRunService.createDryRun(request, user)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @PostMapping("/{dryRunId}/execute")
    @Operation(summary = "Execute a dry run")
    fun executeDryRun(
        @PathVariable dryRunId: UUID,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<DryRunResponse>> {
        val user = getCurrentUser(authentication)
        val response = dryRunService.executeDryRun(dryRunId, user)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/{dryRunId}")
    @Operation(summary = "Get a dry run by ID")
    fun getDryRun(@PathVariable dryRunId: UUID): ResponseEntity<ApiResponse<DryRunResponse>> {
        val response = dryRunService.getDryRun(dryRunId)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping
    @Operation(summary = "List dry runs")
    fun listDryRuns(
        authentication: Authentication,
        @RequestParam(required = false) reportId: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<DryRunListResponse>> {
        val user = getCurrentUser(authentication)
        val response = dryRunService.listDryRuns(reportId, user.id, page, size)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @DeleteMapping("/{dryRunId}")
    @Operation(summary = "Delete a dry run")
    fun deleteDryRun(
        @PathVariable dryRunId: UUID,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<Unit>> {
        val user = getCurrentUser(authentication)
        dryRunService.deleteDryRun(dryRunId, user)
        return ResponseEntity.ok(ApiResponse.ok(Unit))
    }

    private fun getCurrentUser(authentication: Authentication): User {
        return User(email = authentication.name, passwordHash = "", fullName = authentication.name)
    }
}