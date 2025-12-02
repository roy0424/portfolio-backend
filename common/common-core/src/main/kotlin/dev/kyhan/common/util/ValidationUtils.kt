package dev.kyhan.common.util

object ValidationUtils {
    private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    private val SUBDOMAIN_REGEX = "^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$".toRegex()
    private val DOMAIN_REGEX = "^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)*$".toRegex()
    private val URL_REGEX = "^https?://.+".toRegex(RegexOption.IGNORE_CASE)

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email)

    fun isValidSubdomain(subdomain: String): Boolean = subdomain.length in 3..63 && SUBDOMAIN_REGEX.matches(subdomain)

    fun isValidDomain(domain: String): Boolean = domain.length in 3..253 && DOMAIN_REGEX.matches(domain)

    fun isValidPassword(password: String): Boolean = password.length >= 8

    fun isValidUrl(url: String?): Boolean = !url.isNullOrBlank() && URL_REGEX.matches(url)
}
