package dev.kyhan.common.exception

enum class ErrorCode(
    val code: String,
    val message: String,
    val status: Int
) {
    // Common (1xxx)
    INTERNAL_SERVER_ERROR("1000", "Internal server error", 500),
    INVALID_INPUT("1001", "Invalid input", 400),
    UNAUTHORIZED("1002", "Unauthorized", 401),
    FORBIDDEN("1003", "Forbidden", 403),
    NOT_FOUND("1004", "Resource not found", 404),
    CONFLICT("1005", "Resource conflict", 409),

    // Auth (2xxx)
    INVALID_CREDENTIALS("2000", "Invalid email or password", 401),
    EMAIL_ALREADY_EXISTS("2001", "Email already exists", 409),
    INVALID_TOKEN("2002", "Invalid or expired token", 401),
    TOKEN_EXPIRED("2003", "Token expired", 401),

    // Site (3xxx)
    SITE_NOT_FOUND("3000", "Site not found", 404),
    SITE_ALREADY_EXISTS("3001", "Site already exists", 409),
    DOMAIN_ALREADY_TAKEN("3002", "Domain already taken", 409),

    // Page (4xxx)
    PAGE_NOT_FOUND("4000", "Page not found", 404),
    INVALID_BLOCK_TYPE("4001", "Invalid block type", 400),

    // Project (5xxx)
    PROJECT_NOT_FOUND("5000", "Project not found", 404),

    // Blog (6xxx)
    BLOG_POST_NOT_FOUND("6000", "Blog post not found", 404),

    // Asset (7xxx)
    ASSET_NOT_FOUND("7000", "Asset not found", 404),
    FILE_TOO_LARGE("7001", "File size exceeds limit", 400),
    INVALID_FILE_TYPE("7002", "Invalid file type", 400),
}