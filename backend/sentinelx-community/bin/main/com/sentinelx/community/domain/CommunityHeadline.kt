package com.sentinelx.community.domain

import com.sentinelx.shared.domain.HeadlineDetectionMethod
import com.sentinelx.shared.domain.HeadlineStatus
import com.sentinelx.shared.domain.ReportSeverity
import com.sentinelx.shared.kernel.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "community_headlines",
    indexes = [
        Index(name = "idx_community_headlines_status", columnList = "status"),
        Index(name = "idx_community_headlines_created_at", columnList = "created_at")
    ]
)
class CommunityHeadline(
    title: String,
    summary: String,
    evidenceReportIds: Array<UUID>,
    detectionMethod: HeadlineDetectionMethod
) : BaseEntity() {

    @Column(name = "title", nullable = false, length = 500)
    var title: String = title

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    var summary: String = summary

    @Column(name = "report_count", nullable = false)
    var reportCount: Int = 0

    @Column(name = "simulation_count", nullable = false)
    var simulationCount: Int = 0

    @Column(name = "affected_component", length = 255)
    var affectedComponent: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: HeadlineStatus = HeadlineStatus.DETECTED

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    var severity: ReportSeverity? = null

    @Column(name = "evidence_report_ids", nullable = false, columnDefinition = "UUID[]")
    var evidenceReportIds: Array<UUID> = evidenceReportIds

    @Column(name = "evidence_simulation_ids", columnDefinition = "UUID[]")
    var evidenceSimulationIds: Array<UUID> = emptyArray()

    @Enumerated(EnumType.STRING)
    @Column(name = "detection_method", nullable = false, length = 20)
    var detectionMethod: HeadlineDetectionMethod = detectionMethod

    @Column(name = "resolved_at")
    var resolvedAt: Instant? = null

    constructor() : this("", "", emptyArray(), HeadlineDetectionMethod.TAGS)
}