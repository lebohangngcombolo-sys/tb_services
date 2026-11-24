
package com.tradebridge.notification.provider.email;

import com.tradebridge.common.enums.Channel;
import com.tradebridge.notification.provider.NotificationProvider;
import com.tradebridge.notification.provider.ProviderResult;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class SmtpEmailProvider implements NotificationProvider {

    private static final Logger log =
        LoggerFactory.getLogger(SmtpEmailProvider.class);

    public static final String NAME = "smtp";

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}"
    );

    private final JavaMailSender mailSender;
    private final SmtpProperties properties;

    public SmtpEmailProvider(
        JavaMailSender mailSender,
        SmtpProperties properties) {

        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Channel channel() {
        return Channel.EMAIL;
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public ProviderResult send(
        String destination,
        String subject,
        String body) {

        try {

            String senderAddress = properties.getFromAddress();

            if (senderAddress == null || senderAddress.isBlank()) {
                return ProviderResult.failed(
                    NAME,
                    "SMTP_CONFIGURATION",
                    "SMTP sender address is not configured"
                );
            }

            InternetAddress recipient =
                validateAddress(destination);

            InternetAddress sender =
                validateAddress(senderAddress);

            String fromName = properties.getFromName();

            if (fromName != null && !fromName.isBlank()) {
                sender.setPersonal(
                    fromName,
                    StandardCharsets.UTF_8.name()
                );
            }

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                message,
                false,
                StandardCharsets.UTF_8.name()
            );

            helper.setTo(recipient);
            helper.setFrom(sender);

            helper.setSubject(
                subject == null ? "" : subject
            );

            helper.setText(
                body == null ? "" : body,
                true
            );

            mailSender.send(message);

            String messageId = UUID.randomUUID()
                + "@"
                + domainOf(senderAddress);

            log.info(
                "SMTP email accepted: recipient={}, messageId={}",
                maskEmail(destination),
                messageId
            );

            return ProviderResult.accepted(
                NAME,
                messageId
            );

        } catch (AddressException e) {

            log.warn(
                "Invalid SMTP email address: {}",
                e.getClass().getSimpleName()
            );

            return ProviderResult.failed(
                NAME,
                "INVALID_EMAIL_ADDRESS",
                "Sender or recipient email address is invalid"
            );

        } catch (MessagingException | UnsupportedEncodingException e) {

            log.error(
                "SMTP message construction failed: {}",
                e.getClass().getSimpleName()
            );

            return ProviderResult.failed(
                NAME,
                "SMTP_MESSAGE_ERROR",
                "SMTP message construction failed"
            );

        } catch (MailException e) {

            log.error(
                "SMTP transport failure: {}",
                e.getClass().getSimpleName()
            );

            logNestedFailures(e);

            return ProviderResult.failed(
                NAME,
                "SMTP_TRANSPORT",
                "SMTP transport failure"
            );
        }
    }

    private static InternetAddress validateAddress(String value)
        throws AddressException {

        if (value == null || value.isBlank()) {
            throw new AddressException("Email address is empty");
        }

        String normalized = value.trim();

        InternetAddress address =
            new InternetAddress(normalized, true);

        address.validate();

        if (!normalized.equals(address.getAddress())) {
            throw new AddressException(
                "Unexpected email address format"
            );
        }

        return address;
    }

    private static void logNestedFailures(Throwable exception) {

        Set<Throwable> visited =
            Collections.newSetFromMap(
                new IdentityHashMap<>()
            );

        ArrayDeque<Throwable> pending = new ArrayDeque<>();

        pending.add(exception);

        if (exception instanceof MailSendException sendException) {

            sendException.getFailedMessages()
                .values()
                .stream()
                .filter(failure -> failure != null)
                .forEach(pending::addLast);
        }

        while (!pending.isEmpty()) {

            Throwable failure = pending.removeFirst();

            if (!visited.add(failure)) {
                continue;
            }

            String message = failure.getMessage();

            log.error(
                "SMTP nested exception: type={}, message={}",
                failure.getClass().getSimpleName(),
                redact(message)
            );

            if (failure.getCause() != null) {
                pending.addLast(failure.getCause());
            }

            for (Throwable suppressed : failure.getSuppressed()) {
                pending.addLast(suppressed);
            }

            if (failure instanceof MessagingException messagingException
                && messagingException.getNextException() != null) {

                pending.addLast(
                    messagingException.getNextException()
                );
            }
        }
    }

    private static String redact(String message) {

        if (message == null) {
            return "";
        }

        return EMAIL_PATTERN.matcher(message)
            .replaceAll("[REDACTED_EMAIL]");
    }

    private static String domainOf(String email) {

        int at = email.indexOf('@');

        return at < 0
            ? "localhost"
            : email.substring(at + 1);
    }

    private static String maskEmail(String email) {

        if (email == null) {
            return "[REDACTED]";
        }

        int at = email.indexOf('@');

        if (at <= 2) {
            return "***";
        }

        return email.substring(0, 2)
            + "***"
            + email.substring(at);
    }
}
