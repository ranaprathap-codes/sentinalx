package com.sentinelx.community.domain

import com.sentinelx.shared.domain.RelationshipDetectionMethod
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
    name = "problem_relationships",
    indexes = [
        Index(name = "idx_problem_relationships_report_id_1", columnList = "report_id_1"),
        Index(name = "idx_problem_relationships_report_id_2", columnList = "report_id_2"),
        Index(name = "idx_problem_relationships_similarity", columnList = "similarity_score")
    ]
)
class ProblemRelationship(
    reportId1: UUID,
    reportId2: UUID,
    similarityScore: Double,
    detectedBy: RelationshipDetectionMethod
) : BaseEntity() {

    @Column(name = "report_id_1", nullable = false)
    var reportId1: UUID = reportId1

    @Column(name = "report_id_2", nullable = false)
    var reportId2: UUID = reportId2

    @Column(name = "similarity_score", nullable = false, precision = 5, scale = 4)
    var similarityScore: Double = similarityScore

    @Enumerated(EnumType.STRING)
    @Column(name = "detected_by", nullable = false, length = 20)
    var detectedBy: RelationshipDetectionMethod = detectedBy

    @Column(name = "evidence", columnDefinition = "JSONB")
    var evidence: String = "{}"

    @Column(name = "confirmed")
    var confirmed: Boolean? = null

    @Column(name = "confirmed_by")
    var confirmedBy: UUID? = null

    @Column(name = "confirmed_at")
    var confirmedAt: Instant? = null

    constructor() : this(UUID.randomUUID(), UUID.randomUUID(), 0.0, RelationshipDetectionMethod.TAGS)
}