package com.anastasia.Anastasia_BackEnd.UnitTests.core.notification.service;

import com.anastasia.Anastasia_BackEnd.core.notification.config.SesSnsWebhookProperties;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.SesSnsMessage;
import com.anastasia.Anastasia_BackEnd.core.notification.service.InvalidSesSnsMessageException;
import com.anastasia.Anastasia_BackEnd.core.notification.service.SesSnsMessageVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestOperations;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SesSnsMessageVerifierTest {

    private static final String TOPIC_ARN = "arn:aws:sns:us-east-1:123456789012:ses-events";
    private static final String CERT_URL = "https://sns.us-east-1.amazonaws.com/SimpleNotificationService-test.pem";

    @Test
    void verify_shouldAcceptValidSignatureFromAllowedTopic() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        SesSnsWebhookProperties properties = new SesSnsWebhookProperties();
        properties.setTopicArns(List.of(TOPIC_ARN));
        SesSnsMessageVerifier verifier = new TestSesSnsMessageVerifier(properties, keyPair.getPublic());

        String message = "{\"notificationType\":\"Bounce\"}";
        String messageId = "84a33e93-1cf0-4f2d-92ea-57ea15915dac";
        String timestamp = "2026-05-29T16:30:00.000Z";
        String canonical = "Message\n"
                + message + "\n"
                + "MessageId\n"
                + messageId + "\n"
                + "Timestamp\n"
                + timestamp + "\n"
                + "TopicArn\n"
                + TOPIC_ARN + "\n"
                + "Type\n"
                + "Notification\n";

        SesSnsMessage snsMessage = new SesSnsMessage(
                "Notification",
                messageId,
                message,
                timestamp,
                TOPIC_ARN,
                null,
                null,
                null,
                "2",
                sign(canonical, keyPair.getPrivate(), "SHA256withRSA"),
                CERT_URL
        );

        assertThatCode(() -> verifier.verify(snsMessage, "Notification"))
                .doesNotThrowAnyException();
    }

    @Test
    void verify_shouldRejectMessagesFromUnexpectedTopic() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        SesSnsWebhookProperties properties = new SesSnsWebhookProperties();
        properties.setTopicArns(List.of("arn:aws:sns:us-east-1:123456789012:different-topic"));
        SesSnsMessageVerifier verifier = new TestSesSnsMessageVerifier(properties, keyPair.getPublic());

        SesSnsMessage snsMessage = new SesSnsMessage(
                "Notification",
                "84a33e93-1cf0-4f2d-92ea-57ea15915dac",
                "{\"notificationType\":\"Complaint\"}",
                "2026-05-29T16:30:00.000Z",
                TOPIC_ARN,
                null,
                null,
                null,
                "2",
                "unused",
                CERT_URL
        );

        assertThatThrownBy(() -> verifier.verify(snsMessage, "Notification"))
                .isInstanceOf(InvalidSesSnsMessageException.class)
                .extracting(ex -> ((InvalidSesSnsMessageException) ex).status())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private static String sign(String canonicalMessage, PrivateKey privateKey, String algorithm) throws Exception {
        Signature signature = Signature.getInstance(algorithm);
        signature.initSign(privateKey);
        signature.update(canonicalMessage.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private static final class TestSesSnsMessageVerifier extends SesSnsMessageVerifier {

        private final X509Certificate certificate;

        private TestSesSnsMessageVerifier(SesSnsWebhookProperties properties, PublicKey publicKey) {
            super(mock(RestOperations.class), properties);
            this.certificate = new StubX509Certificate(publicKey);
        }

        @Override
        protected X509Certificate loadCertificate(String signingCertUrl) {
            return certificate;
        }
    }

    private static final class StubX509Certificate extends X509Certificate {

        private final PublicKey publicKey;

        private StubX509Certificate(PublicKey publicKey) {
            this.publicKey = publicKey;
        }

        @Override
        public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {
        }

        @Override
        public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException {
        }

        @Override
        public int getVersion() {
            return 3;
        }

        @Override
        public BigInteger getSerialNumber() {
            return BigInteger.ONE;
        }

        @Override
        public Principal getIssuerDN() {
            return () -> "CN=SNS";
        }

        @Override
        public Principal getSubjectDN() {
            return () -> "CN=SNS";
        }

        @Override
        public Date getNotBefore() {
            return new Date(0L);
        }

        @Override
        public Date getNotAfter() {
            return new Date(Long.MAX_VALUE);
        }

        @Override
        public byte[] getTBSCertificate() throws CertificateEncodingException {
            return new byte[0];
        }

        @Override
        public byte[] getSignature() {
            return new byte[0];
        }

        @Override
        public String getSigAlgName() {
            return "SHA256withRSA";
        }

        @Override
        public String getSigAlgOID() {
            return "1.2.840.113549.1.1.11";
        }

        @Override
        public byte[] getSigAlgParams() {
            return new byte[0];
        }

        @Override
        public boolean[] getIssuerUniqueID() {
            return null;
        }

        @Override
        public boolean[] getSubjectUniqueID() {
            return null;
        }

        @Override
        public boolean[] getKeyUsage() {
            return null;
        }

        @Override
        public int getBasicConstraints() {
            return -1;
        }

        @Override
        public byte[] getEncoded() throws CertificateEncodingException {
            return new byte[0];
        }

        @Override
        public void verify(PublicKey key) {
        }

        @Override
        public void verify(PublicKey key, String sigProvider) {
        }

        @Override
        public String toString() {
            return "StubX509Certificate";
        }

        @Override
        public PublicKey getPublicKey() {
            return publicKey;
        }

        @Override
        public Set<String> getCriticalExtensionOIDs() {
            return null;
        }

        @Override
        public Set<String> getNonCriticalExtensionOIDs() {
            return null;
        }

        @Override
        public byte[] getExtensionValue(String oid) {
            return null;
        }

        @Override
        public boolean hasUnsupportedCriticalExtension() {
            return false;
        }

        @Override
        public List<String> getExtendedKeyUsage() throws CertificateParsingException {
            return null;
        }

        @Override
        public Collection<List<?>> getSubjectAlternativeNames() throws CertificateParsingException {
            return null;
        }

        @Override
        public Collection<List<?>> getIssuerAlternativeNames() throws CertificateParsingException {
            return null;
        }
    }
}
