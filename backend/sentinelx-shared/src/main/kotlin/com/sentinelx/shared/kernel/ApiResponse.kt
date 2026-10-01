package com.sentinelx.shared.kernel

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
    val meta: ApiMeta? = null,
    val timestamp: Instant = Instant.now()
) {
    companion object {
        fun <T> ok(data: T, meta: ApiMeta? = null): ApiResponse<T> = ApiResponse(true, data = data, meta = meta)
        fun <T> error(error: ApiError): ApiResponse<T> = ApiResponse(false, error = error)
    }
}

data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, Any>? = null
)

data class ApiMeta(
    val correlationId: UUID,
    val page: PageMeta? = null,
    val requestDurationMs: Long? = null
)

data class PageMeta(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class PageRequest(
    val page: Int = 0,
    val size: Int = 20,
    val sort: String? = null
) {
    companion object {
        const val MAX_SIZE = 100
    }
}