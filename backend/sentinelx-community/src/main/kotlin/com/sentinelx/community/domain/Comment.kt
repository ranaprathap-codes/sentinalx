package com.sentinelx.community.domain

import com.sentinelx.shared.kernel.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "comments",
    indexes = [
        Index(name = "idx_comments_report_id", columnList = "report_id"),
        Index(name = "idx_comments_user_id", columnList = "user_id"),
        Index(name = "idx_comments_parent_comment_id", columnList = "parent_comment_id")
    ]
)
class Comment(
    reportId: UUID,
    userId: UUID,
    content: String,
    parentCommentId: UUID? = null
) : BaseEntity() {

    @Column(name = "report_id", nullable = false)
    var reportId: UUID = reportId

    @Column(name = "user_id", nullable = false)
    var userId: UUID = userId

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String = content

    @Column(name = "parent_comment_id")
    var parentCommentId: UUID? = parentCommentId

    @Column(name = "is_system_message", nullable = false)
    var isSystemMessage: Boolean = false

    @Column(name = "metadata", columnDefinition = "JSONB")
    var metadata: String = "{}"

    constructor() : this(UUID.randomUUID(), UUID.randomUUID(), "")
}