package com.anastasia.Anastasia_BackEnd.core.notification.service;

import com.anastasia.Anastasia_BackEnd.core.notification.config.SesSnsWebhookProperties;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.SesSnsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestOperations;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class SesSnsMessageVerifier {

    private static final String MESSAGE_TYPE_NOTIFICATION = "Notification";
    private static final String MESSAGE_TYPE_SUBSCRIPTION_CONFIRMATION = "SubscriptionConfirmation";
    private static final String MESSAGE_TYPE_UNSUBSCRIBE_CONFIRMATION = "UnsubscribeConfirmation";
    private static final String SIGNATURE_VERSION_1 = "1";
    private static final String SIGNATURE_VERSION_2 = "2";
    private static final Pattern SNS_CERT_HOST_PATTERN =
            Pattern.compile("^sns\\.[a-z0-9-]+\\.amazonaws\\.com(?:\\.cn)?$");

    private final RestOperations snsRestOperations;
    private final SesSnsWebhookProperties properties;

    public void verify(SesSnsMessage snsMessage, String snsMessageTypeHeader) {
        String messageType = requireText(snsMessage.type(), "SNS message missing Type", HttpStatus.BAD_REQUEST);
        if (StringUtils.hasText(snsMessageTypeHeader) && !messageType.equals(snsMessageTypeHeader)) {
            throw new InvalidSesSnsMessageException(HttpStatus.BAD_REQUEST, "SNS message type header mismatch");
        }

        requireSupportedType(messageType);
        validateUuid(snsMessage.messageId(), "SNS message missing or invalid MessageId");
        requireAllowedTopicArn(snsMessage.topicArn());
        validateTimestamp(snsMessage.timestamp());
        String signatureVersion = requireText(
                snsMessage.signatureVersion(),
                "SNS message missing SignatureVersion",
                HttpStatus.BAD_REQUEST
        );
        String algorithm = signatureAlgorithm(signatureVersion);
        String signature = requireText(snsMessage.signature(), "SNS message missing Signature", HttpStatus.BAD_REQUEST);
        String signingCertUrl = requireText(
                snsMessage.signingCertURL(),
                "SNS message missing SigningCertURL",
                HttpStatus.BAD_REQUEST
        );
        validateSigningCertUrl(signingCertUrl);

        if (MESSAGE_TYPE_SUBSCRIPTION_CONFIRMATION.equals(messageType)
                || MESSAGE_TYPE_UNSUBSCRIBE_CONFIRMATION.equals(messageType)) {
            requireText(snsMessage.token(), "SNS confirmation missing Token", HttpStatus.BAD_REQUEST);
            requireText(snsMessage.subscribeURL(), "SNS confirmation missing SubscribeURL", HttpStatus.BAD_REQUEST);
        }

        verifySignature(snsMessage, signature, signingCertUrl, algorithm);
    }

    protected X509Certificate loadCertificate(String signingCertUrl) {
        ResponseEntity<byte[]> response = snsRestOperations.getForEntity(signingCertUrl, byte[].class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().length == 0) {
            throw new InvalidSesSnsMessageException(HttpStatus.FORBIDDEN, "SNS signing certificate could not be loaded");
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(response.getBody())) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(inputStream);
            certificate.checkValidity();
            return certificate;
        } catch (InvalidSesSnsMessageException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to load SNS signing certificate: {}", ex.getMessage());
            throw new InvalidSesSnsMessageException(HttpStatus.FORBIDDEN, "SNS signing certificate is invalid");
        }
    }

    private void verifySignature(SesSnsMessage snsMessage,
                                 String signature,
                                 String signingCertUrl,
                                 String algorithm) {
        try {
            X509Certificate certificate = loadCertificate(signingCertUrl);
            Signature verifier = Signature.getInstance(algorithm);
            verifier.initVerify(certificate);
            verifier.update(buildCanonicalMessage(snsMessage).getBytes(StandardCharsets.UTF_8));

            if (!verifier.verify(Base64.getDecoder().decode(signature))) {
                throw new InvalidSesSnsMessageException(HttpStatus.FORBIDDEN, "SNS signature verification failed");
            }
        } catch (InvalidSesSnsMessageException ex) {
            throw ex;
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            log.warn("Failed to verify SNS signature: {}", ex.getMessage());
            throw new InvalidSesSnsMessageException(HttpStatus.FORBIDDEN, "SNS signature verification failed");
        }
    }

    private String buildCanonicalMessage(SesSnsMessage snsMessage) {
        StringBuilder canonical = new StringBuilder();
        appendCanonicalField(canonical, "Message", snsMessage.message());
        appendCanonicalField(canonical, "MessageId", snsMessage.messageId());

        if (MESSAGE_TYPE_NOTIFICATION.equals(snsMessage.type())) {
            if (StringUtils.hasText(snsMessage.subject())) {
                appendCanonicalField(canonical, "Subject", snsMessage.subject());
            }
        } else {
            appendCanonicalField(canonical, "SubscribeURL", snsMessage.subscribeURL());
        }

        appendCanonicalField(canonical, "Timestamp", snsMessage.timestamp());

        if (!MESSAGE_TYPE_NOTIFICATION.equals(snsMessage.type())) {
            appendCanonicalField(canonical, "Token", snsMessage.token());
        }

        appendCanonicalField(canonical, "TopicArn", snsMessage.topicArn());
        appendCanonicalField(canonical, "Type", snsMessage.type());
        return canonical.toString();
    }

    private void appendCanonicalField(StringBuilder canonical, String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        canonical.append(fieldName).append('\n').append(value).append('\n');
    }

    private void requireSupportedType(String messageType) {
        List<String> supportedTypes = List.of(
                MESSAGE_TYPE_NOTIFICATION,
                MESSAGE_TYPE_SUBSCRIPTION_CONFIRMATION,
                MESSAGE_TYPE_UNSUBSCRIBE_CONFIRMATION
        );
        if (!supportedTypes.contains(messageType)) {
            throw new InvalidSesSnsMessageException(HttpStatus.BAD_REQUEST, "Unsupported SNS message type");
        }
    }

    private void requireAllowedTopicArn(String topicArn) {
        String normalizedTopicArn = requireText(topicArn, "SNS message missing TopicArn", HttpStatus.BAD_REQUEST);
        List<String> allowedTopicArns = properties.getTopicArns() == null
                ? List.of()
                : properties.getTopicArns().stream().filter(StringUtils::hasText).toList();
        if (allowedTopicArns.isEmpty()) {
            log.error("SES SNS webhook rejected because notification.email.ses-sns.topic-arns is empty");
            throw new InvalidSesSnsMessageException(HttpStatus.FORBIDDEN, "SNS topic is not allowed");
        }

        boolean allowed = allowedTopicArns.stream().anyMatch(normalizedTopicArn::equals);
        if (!allowed) {
            throw new InvalidSesSnsMessageException(HttpStatus.FORBIDDEN, "SNS topic is not allowed");
        }
    }

    private void validateTimestamp(String timestamp) {
        requireText(timestamp, "SNS message missing Timestamp", HttpStatus.BAD_REQUEST);
        try {
            Instant.parse(timestamp);
        } catch (DateTimeParseException ex) {
            throw new InvalidSesSnsMessageException(HttpStatus.BAD_REQUEST, "SNS message has invalid Timestamp");
        }
    }

    private void validateUuid(String value, String message) {
        String normalized = requireText(value, message, HttpStatus.BAD_REQUEST);
        try {
            UUID.fromString(normalized);
        } catch (IllegalArgumentException ex) {
            throw new InvalidSesSnsMessageException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private void validateSigningCertUrl(String rawUrl) {
        java.net.URI uri;
        try {
            uri = java.net.URI.create(rawUrl);
        } catch (IllegalArgumentException ex) {
            throw new InvalidSesSnsMessageException(HttpStatus.BAD_REQUEST, "SNS SigningCertURL is invalid");
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !StringUtils.hasText(uri.getHost())
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new InvalidSesSnsMessageException(HttpStatus.BAD_REQUEST, "SNS SigningCertURL is invalid");
        }

        String normalizedHost = uri.getHost().toLowerCase(Locale.ROOT);
        String normalizedPath = uri.getPath() == null ? "" : uri.getPath();
        if (!SNS_CERT_HOST_PATTERN.matcher(normalizedHost).matches()
                || !normalizedPath.startsWith("/SimpleNotificationService-")
                || !normalizedPath.endsWith(".pem")) {
            throw new InvalidSesSnsMessageException(HttpStatus.FORBIDDEN, "SNS SigningCertURL is not trusted");
        }
    }

    private String signatureAlgorithm(String signatureVersion) {
        return switch (signatureVersion) {
            case SIGNATURE_VERSION_1 -> "SHA1withRSA";
            case SIGNATURE_VERSION_2 -> "SHA256withRSA";
            default -> throw new InvalidSesSnsMessageException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported SNS SignatureVersion"
            );
        };
    }

    private String requireText(String value, String message, HttpStatus status) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidSesSnsMessageException(status, message);
        }
        return value;
    }
}
