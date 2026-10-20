package com.sentinelx.community.controller

import com.sentinelx.auth.domain.User
import com.sentinelx.community.dto.CommentResponse
import com.sentinelx.community.dto.CommunityHeadlineResponse
import com.sentinelx.community.dto.CreateCommentRequest
import com.sentinelx.community.dto.CreateReportRequest
import com.sentinelx.community.dto.HeadlineListResponse
import com.sentinelx.community.dto.ReportListResponse
import com.sentinelx.community.dto.ReportResponse
import com.sentinelx.community.dto.UpdateReportRequest
import com.sentinelx.community.service.CommunityService
import com.sentinelx.shared.domain.ReportStatus
import com.sentinelx.shared.domain.ReportType
import com.sentinelx.shared.kernel.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/community")
@Tag(name = "Community", description = "Engineering community: flaw reports, coder blocks, feature proposals")
class CommunityController(
    private val communityService: CommunityService
) {

    // Reports
    @PostMapping("/reports")
    @Operation(summary = "Create a new report (flaw, coder block, or feature proposal)")
    fun createReport(
        @Valid @RequestBody request: CreateReportRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<ReportResponse>> {
        val user = getCurrentUser(authentication)
        val response = communityService.createReport(request, user)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/reports")
    @Operation(summary = "List reports with filters")
    fun listReports(
        @RequestParam(required = false) type: ReportType?,
        @RequestParam(required = false) status: ReportStatus?,
        @RequestParam(required = false) component: String?,
        @RequestParam(required = false) tag: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<ReportListResponse>> {
        val response = communityService.listReports(type, status, component, tag, null, page, size)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/reports/my")
    @Operation(summary = "List current user's reports")
    fun listMyReports(
        authentication: Authentication,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<ReportListResponse>> {
        val user = getCurrentUser(authentication)
        val response = communityService.listReports(null, null, null, null, user.id, page, size)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/reports/{reportId}")
    @Operation(summary = "Get a report by ID")
    fun getReport(@PathVariable reportId: UUID): ResponseEntity<ApiResponse<ReportResponse>> {
        val response = communityService.getReport(reportId)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @PutMapping("/reports/{reportId}")
    @Operation(summary = "Update a report")
    fun updateReport(
        @PathVariable reportId: UUID,
        @Valid @RequestBody request: UpdateReportRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<ReportResponse>> {
        val user = getCurrentUser(authentication)
        val response = communityService.updateReport(reportId, request, user)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @DeleteMapping("/reports/{reportId}")
    @Operation(summary = "Delete a report")
    fun deleteReport(
        @PathVariable reportId: UUID,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<Unit>> {
        val user = getCurrentUser(authentication)
        communityService.deleteReport(reportId, user)
        return ResponseEntity.ok(ApiResponse.ok(Unit))
    }

    // Comments
    @PostMapping("/reports/{reportId}/comments")
    @Operation(summary = "Add a comment to a report")
    fun addComment(
        @PathVariable reportId: UUID,
        @Valid @RequestBody request: CreateCommentRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<CommentResponse>> {
        val user = getCurrentUser(authentication)
        val response = communityService.addComment(reportId, request, user)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/reports/{reportId}/comments")
    @Operation(summary = "Get comments for a report")
    fun getComments(@PathVariable reportId: UUID): ResponseEntity<ApiResponse<List<CommentResponse>>> {
        val response = communityService.getComments(reportId)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    // Headlines
    @GetMapping("/headlines")
    @Operation(summary = "List community headlines")
    fun listHeadlines(
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<HeadlineListResponse>> {
        val response = communityService.listHeadlines(status, page, size)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @GetMapping("/headlines/{headlineId}")
    @Operation(summary = "Get a headline by ID")
    fun getHeadline(@PathVariable headlineId: UUID): ResponseEntity<ApiResponse<CommunityHeadlineResponse>> {
        val response = communityService.getHeadline(headlineId)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }

    @PutMapping("/headlines/{headlineId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    @Operation(summary = "Update headline status")
    fun updateHeadlineStatus(
        @PathVariable headlineId: UUID,
        @RequestParam status: String
    ): ResponseEntity<ApiResponse<Unit>> {
        // Implementation would update headline status
        return ResponseEntity.ok(ApiResponse.ok(Unit))
    }

    private fun getCurrentUser(authentication: Authentication): User {
        return User(email = authentication.name, passwordHash = "", fullName = authentication.name)
    }
}