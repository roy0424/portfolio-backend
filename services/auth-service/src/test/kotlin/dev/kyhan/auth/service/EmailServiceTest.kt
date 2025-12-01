package dev.kyhan.auth.service

import dev.kyhan.auth.config.MailProperties
import dev.kyhan.common.exception.BusinessException
import dev.kyhan.common.exception.ErrorCode
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mail.javamail.JavaMailSender
import reactor.test.StepVerifier

@DisplayName("EmailService 테스트")
class EmailServiceTest {
    private lateinit var mailSender: JavaMailSender
    private lateinit var mailProperties: MailProperties
    private lateinit var service: EmailService

    private val testEmail = "test@example.com"
    private val testCode = "123456"

    @BeforeEach
    fun setUp() {
        mailSender = mockk(relaxed = true)
        mailProperties =
            MailProperties(
                fromAddress = "noreply@example.com",
                fromName = "Test Service",
            )

        service = EmailService(mailSender, mailProperties)
    }

    @Nested
    @DisplayName("sendVerificationEmail 테스트")
    inner class SendVerificationEmailTest {
        @Test
        @DisplayName("성공: 이메일 전송 성공")
        fun sendVerificationEmail_Success() {
            // Given
            val mimeMessage = mockk<MimeMessage>(relaxed = true)
            every { mailSender.createMimeMessage() } returns mimeMessage
            justRun { mailSender.send(any<MimeMessage>()) }

            // When & Then
            StepVerifier
                .create(service.sendVerificationEmail(testEmail, testCode))
                .verifyComplete()

            verify(exactly = 1) { mailSender.createMimeMessage() }
            verify(exactly = 1) { mailSender.send(any<MimeMessage>()) }
        }

        @Test
        @DisplayName("실패: 이메일 전송 실패")
        fun sendVerificationEmail_SendFailure() {
            // Given
            val mimeMessage = mockk<MimeMessage>(relaxed = true)
            every { mailSender.createMimeMessage() } returns mimeMessage
            every { mailSender.send(any<MimeMessage>()) } throws RuntimeException("SMTP server error")

            // When & Then
            StepVerifier
                .create(service.sendVerificationEmail(testEmail, testCode))
                .expectErrorMatches { error ->
                    error is BusinessException &&
                        error.errorCode == ErrorCode.EMAIL_SEND_FAILED
                }.verify()

            verify(exactly = 1) { mailSender.createMimeMessage() }
            verify(exactly = 1) { mailSender.send(any<MimeMessage>()) }
        }

        @Test
        @DisplayName("실패: MIME 메시지 생성 실패")
        fun sendVerificationEmail_MimeMessageCreationFailure() {
            // Given
            every { mailSender.createMimeMessage() } throws RuntimeException("Mail sender error")

            // When & Then
            StepVerifier
                .create(service.sendVerificationEmail(testEmail, testCode))
                .expectErrorMatches { error ->
                    error is BusinessException &&
                        error.errorCode == ErrorCode.EMAIL_SEND_FAILED
                }.verify()

            verify(exactly = 1) { mailSender.createMimeMessage() }
        }
    }
}
