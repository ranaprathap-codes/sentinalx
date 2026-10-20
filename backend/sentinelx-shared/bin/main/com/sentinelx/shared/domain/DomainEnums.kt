package com.sentinelx.shared.domain

enum class UserRole(val authority: String) {
    ADMIN("ROLE_ADMIN"),
    ANALYST("ROLE_ANALYST"),
    DEVELOPER("ROLE_DEVELOPER"),
    VIEWER("ROLE_VIEWER");

    companion object {
        fun fromAuthority(authority: String): UserRole? = values().firstOrNull { it.authority == authority }
    }
}

enum class TransactionStatus {
    CREATED, PROCESSING, RISK_REVIEW, AUTHORIZED, COMPLETED,
    BLOCKED, FAILED, CANCELLED, REFUNDED
}

enum class RiskDecision {
    ALLOW, REVIEW, BLOCK
}

enum class ReportType {
    FLAW_REPORT, CODER_BLOCK, FEATURE_PROPOSAL
}

enum class ReportSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class ReportStatus {
    OPEN, INVESTIGATING, IN_DEVELOPMENT, TESTING, RESOLVED, CLOSED, WONT_FIX
}

enum class ScenarioCategory {
    FRAUD_PATTERN, VELOCITY_ATTACK, ACCOUNT_TAKEOVER, API_ABUSE,
    TRAFFIC_SPIKE, SERVICE_FAILURE, DATA_EXFILTRATION, CUSTOM
}

enum class IncidentType {
    ERROR_SPIKE, LATENCY_SPIKE, SERVICE_FAILURE, DATABASE_ISSUE, CACHE_ISSUE, EXTERNAL_DEPENDENCY
}

enum class IncidentSeverity {
    SEV1, SEV2, SEV3, SEV4
}

enum class IncidentStatus {
    DETECTED, INVESTIGATING, RESOLVED
}

enum class DryRunStatus {
    PENDING, RUNNING, COMPLETED, FAILED
}

enum class SimulationStatus {
    RUNNING, COMPLETED, FAILED, CANCELLED
}

enum class RelationshipDetectionMethod {
    TAGS, EMBEDDINGS, MANUAL, AI
}

enum class HeadlineStatus {
    DETECTED, INVESTIGATING, IN_DEVELOPMENT, TESTING, RESOLVED
}

enum class HeadlineDetectionMethod {
    TAGS, EMBEDDINGS, HYBRID
}