package dev.kyhan.common.util

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("ValidationUtils 테스트")
class ValidationUtilsTest {
    @Nested
    @DisplayName("isValidEmail 테스트")
    inner class IsValidEmailTest {
        @Test
        @DisplayName("성공: 유효한 이메일 형식")
        fun isValidEmail_ValidFormats() {
            // Standard formats
            assertTrue(ValidationUtils.isValidEmail("test@example.com"))
            assertTrue(ValidationUtils.isValidEmail("user.name@example.com"))
            assertTrue(ValidationUtils.isValidEmail("user+tag@example.com"))
            assertTrue(ValidationUtils.isValidEmail("user_name@example.com"))
            assertTrue(ValidationUtils.isValidEmail("user-name@example.com"))

            // Subdomain
            assertTrue(ValidationUtils.isValidEmail("test@mail.example.com"))

            // Numbers
            assertTrue(ValidationUtils.isValidEmail("user123@example.com"))
            assertTrue(ValidationUtils.isValidEmail("123@example.com"))

            // Multiple dots in domain
            assertTrue(ValidationUtils.isValidEmail("test@co.uk"))
            assertTrue(ValidationUtils.isValidEmail("test@mail.co.uk"))
        }

        @Test
        @DisplayName("실패: 잘못된 이메일 형식")
        fun isValidEmail_InvalidFormats() {
            // Missing @
            assertFalse(ValidationUtils.isValidEmail("testexample.com"))

            // Missing domain
            assertFalse(ValidationUtils.isValidEmail("test@"))

            // Missing local part
            assertFalse(ValidationUtils.isValidEmail("@example.com"))

            // Missing TLD
            assertFalse(ValidationUtils.isValidEmail("test@example"))

            // Multiple @
            assertFalse(ValidationUtils.isValidEmail("test@@example.com"))
            assertFalse(ValidationUtils.isValidEmail("test@test@example.com"))

            // Invalid characters
            assertFalse(ValidationUtils.isValidEmail("test user@example.com"))
            assertFalse(ValidationUtils.isValidEmail("test@exam ple.com"))

            // Empty string
            assertFalse(ValidationUtils.isValidEmail(""))

            // Only special characters
            assertFalse(ValidationUtils.isValidEmail("@@@"))
        }
    }

    @Nested
    @DisplayName("isValidSubdomain 테스트")
    inner class IsValidSubdomainTest {
        @Test
        @DisplayName("성공: 유효한 서브도메인")
        fun isValidSubdomain_Valid() {
            // Minimum length (3)
            assertTrue(ValidationUtils.isValidSubdomain("abc"))
            assertTrue(ValidationUtils.isValidSubdomain("a1b"))

            // Standard formats
            assertTrue(ValidationUtils.isValidSubdomain("mysite"))
            assertTrue(ValidationUtils.isValidSubdomain("my-site"))
            assertTrue(ValidationUtils.isValidSubdomain("mysite123"))
            assertTrue(ValidationUtils.isValidSubdomain("123site"))

            // With hyphens
            assertTrue(ValidationUtils.isValidSubdomain("my-awesome-site"))
            assertTrue(ValidationUtils.isValidSubdomain("test-123"))

            // Maximum length (63)
            assertTrue(
                ValidationUtils.isValidSubdomain(
                    "a" + "b".repeat(61) + "c", // 63 characters
                ),
            )
        }

        @Test
        @DisplayName("실패: 잘못된 서브도메인")
        fun isValidSubdomain_Invalid() {
            // Too short (< 3)
            assertFalse(ValidationUtils.isValidSubdomain("ab"))
            assertFalse(ValidationUtils.isValidSubdomain("a"))
            assertFalse(ValidationUtils.isValidSubdomain(""))

            // Too long (> 63)
            assertFalse(ValidationUtils.isValidSubdomain("a".repeat(64)))

            // Starts with hyphen
            assertFalse(ValidationUtils.isValidSubdomain("-mysite"))

            // Ends with hyphen
            assertFalse(ValidationUtils.isValidSubdomain("mysite-"))

            // Contains uppercase
            assertFalse(ValidationUtils.isValidSubdomain("MyCase"))
            assertFalse(ValidationUtils.isValidSubdomain("UPPERCASE"))

            // Contains special characters
            assertFalse(ValidationUtils.isValidSubdomain("my_site"))
            assertFalse(ValidationUtils.isValidSubdomain("my.site"))
            assertFalse(ValidationUtils.isValidSubdomain("my site"))
            assertFalse(ValidationUtils.isValidSubdomain("my@site"))

            // Only hyphen
            assertFalse(ValidationUtils.isValidSubdomain("---"))

            // Consecutive hyphens in middle is ok, but not at start/end
            assertTrue(ValidationUtils.isValidSubdomain("my--site"))
        }
    }

    @Nested
    @DisplayName("isValidDomain 테스트")
    inner class IsValidDomainTest {
        @Test
        @DisplayName("성공: 유효한 도메인")
        fun isValidDomain_Valid() {
            // Minimum length (3) - single level
            assertTrue(ValidationUtils.isValidDomain("abc"))

            // Single level domains
            assertTrue(ValidationUtils.isValidDomain("example"))
            assertTrue(ValidationUtils.isValidDomain("test123"))

            // Multi-level domains
            assertTrue(ValidationUtils.isValidDomain("example.com"))
            assertTrue(ValidationUtils.isValidDomain("mail.example.com"))
            assertTrue(ValidationUtils.isValidDomain("sub.mail.example.com"))

            // With hyphens
            assertTrue(ValidationUtils.isValidDomain("my-domain.com"))
            assertTrue(ValidationUtils.isValidDomain("my-site.co.uk"))

            // Numbers
            assertTrue(ValidationUtils.isValidDomain("123.456.789"))

            // Maximum length (253)
            val maxDomain = "a".repeat(63) + "." + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61)
            assertTrue(ValidationUtils.isValidDomain(maxDomain)) // 63+1+63+1+63+1+61 = 253
        }

        @Test
        @DisplayName("실패: 잘못된 도메인")
        fun isValidDomain_Invalid() {
            // Too short (< 3)
            assertFalse(ValidationUtils.isValidDomain("ab"))
            assertFalse(ValidationUtils.isValidDomain("a"))
            assertFalse(ValidationUtils.isValidDomain(""))

            // Too long (> 253)
            assertFalse(ValidationUtils.isValidDomain("a".repeat(254)))

            // Starts with hyphen
            assertFalse(ValidationUtils.isValidDomain("-example.com"))

            // Ends with hyphen
            assertFalse(ValidationUtils.isValidDomain("example-.com"))

            // Starts with dot
            assertFalse(ValidationUtils.isValidDomain(".example.com"))

            // Ends with dot
            assertFalse(ValidationUtils.isValidDomain("example.com."))

            // Contains uppercase
            assertFalse(ValidationUtils.isValidDomain("Example.com"))
            assertFalse(ValidationUtils.isValidDomain("example.Com"))

            // Contains special characters
            assertFalse(ValidationUtils.isValidDomain("my_domain.com"))
            assertFalse(ValidationUtils.isValidDomain("my domain.com"))
            assertFalse(ValidationUtils.isValidDomain("my@domain.com"))

            // Double dots
            assertFalse(ValidationUtils.isValidDomain("example..com"))

            // Only dots
            assertFalse(ValidationUtils.isValidDomain("..."))
        }
    }

    @Nested
    @DisplayName("isValidPassword 테스트")
    inner class IsValidPasswordTest {
        @Test
        @DisplayName("성공: 유효한 비밀번호")
        fun isValidPassword_Valid() {
            // Minimum length (8)
            assertTrue(ValidationUtils.isValidPassword("12345678"))
            assertTrue(ValidationUtils.isValidPassword("password"))

            // Standard passwords
            assertTrue(ValidationUtils.isValidPassword("MyPassword123"))
            assertTrue(ValidationUtils.isValidPassword("P@ssw0rd!"))

            // Long passwords
            assertTrue(ValidationUtils.isValidPassword("a".repeat(100)))

            // With special characters
            assertTrue(ValidationUtils.isValidPassword("password!@#$"))

            // With spaces
            assertTrue(ValidationUtils.isValidPassword("my password"))
        }

        @Test
        @DisplayName("실패: 잘못된 비밀번호")
        fun isValidPassword_Invalid() {
            // Too short (< 8)
            assertFalse(ValidationUtils.isValidPassword("1234567"))
            assertFalse(ValidationUtils.isValidPassword("pass"))
            assertFalse(ValidationUtils.isValidPassword("a"))
            assertFalse(ValidationUtils.isValidPassword(""))
        }
    }
}
