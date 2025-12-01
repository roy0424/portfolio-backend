package dev.kyhan.common.exception

class NotFoundException(
    errorCode: ErrorCode = ErrorCode.NOT_FOUND,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)

class UnauthorizedException(
    errorCode: ErrorCode = ErrorCode.UNAUTHORIZED,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)

class ForbiddenException(
    errorCode: ErrorCode = ErrorCode.FORBIDDEN,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)

class ConflictException(
    errorCode: ErrorCode = ErrorCode.CONFLICT,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)

class InvalidInputException(
    errorCode: ErrorCode = ErrorCode.INVALID_INPUT,
    message: String = errorCode.message,
) : BusinessException(errorCode, message)
