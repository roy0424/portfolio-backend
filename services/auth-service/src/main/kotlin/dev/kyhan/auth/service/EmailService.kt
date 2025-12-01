package dev.kyhan.auth.service

import dev.kyhan.auth.config.MailProperties
import dev.kyhan.common.exception.BusinessException
import dev.kyhan.common.exception.ErrorCode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.ClassPathResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val mailProperties: MailProperties
) {

    fun sendVerificationEmail(email: String, code: String): Mono<Void> {
        return Mono.fromCallable {
            try {
                val message = mailSender.createMimeMessage()
                val helper = MimeMessageHelper(message, true, "UTF-8")

                helper.setFrom(mailProperties.fromAddress, mailProperties.fromName)
                helper.setTo(email)
                helper.setSubject("Verify your email address")

                val htmlContent = loadEmailTemplate(code)
                helper.setText(htmlContent, true)

                mailSender.send(message)
                logger.info { "Verification email sent to: $email with code: $code" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to send verification email to: $email" }
                throw BusinessException(ErrorCode.EMAIL_SEND_FAILED, "Failed to send verification email", e)
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
            .then()
    }

    private fun loadEmailTemplate(code: String): String {
        return try {
            val resource = ClassPathResource("templates/email-verification.html")
            val template = resource.inputStream.readBytes().toString(StandardCharsets.UTF_8)
            template.replace("{{VERIFICATION_CODE}}", code)
        } catch (e: Exception) {
            logger.error(e) { "Failed to load email template" }
            throw BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to load email template", e)
        }
    }
}