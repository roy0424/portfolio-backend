package dev.kyhan.common.exception

enum class ErrorCode(
    val code: String,
    val message: String,
    val status: Int,
) {
    // Common (1xxx)
    INTERNAL_SERVER_ERROR("1000", "Internal server error", 500),
    INVALID_INPUT("1001", "Invalid input", 400),
    UNAUTHORIZED("1002", "Unauthorized", 401),
    FORBIDDEN("1003", "Forbidden", 403),
    NOT_FOUND("1004", "Resource not found", 404),
    CONFLICT("1005", "Resource conflict", 409),
    INVALID_URL_FORMAT("1006", "Invalid URL format", 400),

    // Auth (2xxx)
    INVALID_CREDENTIALS("2000", "Invalid email or password", 401),
    EMAIL_ALREADY_EXISTS("2001", "Email already exists", 409),
    INVALID_TOKEN("2002", "Invalid or expired code", 401),
    TOKEN_EXPIRED("2003", "Code expired", 401),
    EMAIL_ALREADY_VERIFIED("2004", "Email is already verified", 400),
    INVALID_VERIFICATION_CODE("2005", "Invalid or expired verification code", 400),
    EMAIL_NOT_PROVIDED("2006", "Email address is required", 400),
    INVALID_EMAIL_FORMAT("2007", "Invalid email format", 400),
    EMAIL_UPDATE_NOT_ALLOWED("2008", "Email cannot be changed once set", 400),
    EMAIL_VERIFICATION_REQUIRED("2009", "Email verification required", 403),
    EMAIL_SEND_FAILED("2010", "Failed to send verification email", 500),
    TOO_MANY_REQUESTS("2011", "Too many verification requests. Please try again later", 429),
    VERIFICATION_COOLDOWN("2012", "Please wait before requesting a new verification code", 429),
    USER_PROFILE_NOT_FOUND("2013", "User profile not found", 404),
    USER_PROFILE_DELETED("2014", "User profile has been deleted", 410),

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
