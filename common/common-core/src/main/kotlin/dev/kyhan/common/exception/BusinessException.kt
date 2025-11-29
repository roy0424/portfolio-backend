package dev.kyhan.common.exception

open class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
    override val cause: Throwable? = null
) : RuntimeException(message, cause) {
    val status: Int = errorCode.status
    val code: String = errorCode.code
}