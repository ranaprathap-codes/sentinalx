package com.sentinelx.shared.kernel

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
open class BadRequestException(
    message: String
) : RuntimeException(message)

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class UnauthorizedException(
    message: String = "Unauthorized"
) : RuntimeException(message)

@ResponseStatus(HttpStatus.FORBIDDEN)
class ForbiddenException(
    message: String = "Forbidden"
) : RuntimeException(message)

@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundException(
    message: String
) : RuntimeException(message)

@ResponseStatus(HttpStatus.CONFLICT)
open class ConflictException(
    message: String
) : RuntimeException(message)

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
class ValidationException(
    message: String,
    val errors: Map<String, String> = emptyMap()
) : RuntimeException(message)

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
class RateLimitException(
    message: String = "Rate limit exceeded"
) : RuntimeException(message)

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class InternalServerException(
    message: String
) : RuntimeException(message)

class IdempotencyConflictException(
    message: String = "Request with this idempotency key already processed"
) : ConflictException(message)

class InvalidStateTransitionException(
    message: String
) : BadRequestException(message)

class BusinessRuleViolationException(
    message: String,
    val ruleCode: String
) : BadRequestException(message)