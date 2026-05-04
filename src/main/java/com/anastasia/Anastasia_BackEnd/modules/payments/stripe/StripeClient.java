package com.anastasia.Anastasia_BackEnd.modules.payments.stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Client for interacting with the Stripe API to create checkout sessions.
 * Provides methods for creating one-time payment sessions and subscription sessions.
 * Uses configuration properties for success and cancel URLs.
 * Handles idempotency to prevent duplicate session creation.
 * Returns the created Stripe Session object.
 */
@Component
@RequiredArgsConstructor
public class StripeClient {
    private final Environment environment;

    @Value("${stripe.success-url:}") private String successUrlTemplate;
    @Value("${stripe.cancel-url:}")  private String cancelUrlTemplate;

    public Session createCheckoutSession(String paymentId,
                                         UUID tenantId,
                                         long amountMinor,
                                         String currency,
                                         String purposeLabel,
                                         String idempotencyKey) throws StripeException {

        String successUrl = successUrlTemplate.replace("{PAYMENT_ID}", paymentId);
        String cancelUrl  = cancelUrlTemplate.replace("{PAYMENT_ID}", paymentId);
        String normalizedCurrency = currency.toLowerCase(Locale.ROOT);
        String tenant = tenantId.toString();

        // Metadata for the PaymentIntent
        var paymentIntentData = SessionCreateParams.PaymentIntentData.builder()
                .putMetadata("paymentId", paymentId)
                .putMetadata("tenantId", tenant)
                .putMetadata("purpose", purposeLabel)
                .build();

        // Create the checkout session parameters
        var params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(paymentId)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("paymentId", paymentId)
                .putMetadata("tenantId", tenant)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(normalizedCurrency)
                                .setUnitAmount(amountMinor)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Anastasia: " + purposeLabel)
                                        .build())
                                .build())
                        .build())
                .setPaymentIntentData(paymentIntentData)
                .build();

        var options = buildRequestOptions("checkout:" + tenant + ":" + idempotencyKey);

        return Session.create(params, options);
    }

    public Session createSubscriptionCheckoutSession(String subscriptionId,
                                                     UUID tenantId,
                                                     long amountMinor,
                                                     String currency,
                                                     String purposeLabel,
                                                     String idempotencyKey) throws StripeException {

        String successUrl = successUrlTemplate.replace("{PAYMENT_ID}", subscriptionId);
        String cancelUrl  = cancelUrlTemplate.replace("{PAYMENT_ID}", subscriptionId);
        String normalizedCurrency = currency.toLowerCase(Locale.ROOT);
        String tenant = tenantId.toString();

        var recurring = SessionCreateParams.LineItem.PriceData.Recurring.builder()
                .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                .build();

        var priceData = SessionCreateParams.LineItem.PriceData.builder()
                .setCurrency(normalizedCurrency)
                .setUnitAmount(amountMinor)
                .setRecurring(recurring)
                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName("Anastasia Subscription: " + purposeLabel)
                        .build())
                .build();

        var subscriptionData = SessionCreateParams.SubscriptionData.builder()
                .putMetadata("subscriptionId", subscriptionId)
                .putMetadata("tenantId", tenant)
                .putMetadata("purpose", purposeLabel)
                .build();

        var params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setClientReferenceId(subscriptionId)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("subscriptionId", subscriptionId)
                .putMetadata("tenantId", tenant)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(priceData)
                        .build())
                .setSubscriptionData(subscriptionData)
                .build();

        var options = buildRequestOptions("subscription:" + tenant + ":" + idempotencyKey);

        return Session.create(params, options);
    }

    public Session createOnboardingSubscriptionCheckoutSession(String onboardingSessionId,
                                                               String ownerEmail,
                                                               String priceId,
                                                               String purposeLabel,
                                                               String idempotencyKey) throws StripeException {

        String successUrl = successUrlTemplate.replace("{PAYMENT_ID}", onboardingSessionId);
        String cancelUrl = cancelUrlTemplate.replace("{PAYMENT_ID}", onboardingSessionId);

        var subscriptionData = SessionCreateParams.SubscriptionData.builder()
                .putMetadata("onboardingSessionId", onboardingSessionId)
                .putMetadata("billingContext", "TENANT_ONBOARDING")
                .putMetadata("purpose", purposeLabel)
                .build();

        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setClientReferenceId(onboardingSessionId)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("onboardingSessionId", onboardingSessionId)
                .putMetadata("billingContext", "TENANT_ONBOARDING")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPrice(priceId)
                        .build())
                .setSubscriptionData(subscriptionData);

        if (ownerEmail != null && !ownerEmail.isBlank()) {
            paramsBuilder.setCustomerEmail(ownerEmail.trim());
        }

        var params = paramsBuilder.build();

        var options = buildRequestOptions(
                "onboarding-subscription:" + onboardingSessionId + ":" + idempotencyKey
        );

        return Session.create(params, options);
    }

    public Session retrieveCheckoutSession(String checkoutSessionId) throws StripeException {
        return Session.retrieve(checkoutSessionId, buildReadRequestOptions());
    }

    public Subscription retrieveSubscription(String subscriptionId) throws StripeException {
        return Subscription.retrieve(subscriptionId, buildReadRequestOptions());
    }

    private RequestOptions buildRequestOptions(String idempotencyKey) {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Stripe secret key missing: configure stripe.secret-key or STRIPE_SECRET_KEY"
            );
        }
        if (!apiKey.startsWith("sk_")) {
            throw new IllegalStateException("Invalid Stripe API key format for backend; expected key starting with 'sk_'.");
        }

        return RequestOptions.builder()
                .setApiKey(apiKey)
                .setIdempotencyKey(idempotencyKey)
                .build();
    }

    private RequestOptions buildReadRequestOptions() {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Stripe secret key missing: configure stripe.secret-key or STRIPE_SECRET_KEY"
            );
        }
        if (!apiKey.startsWith("sk_")) {
            throw new IllegalStateException("Invalid Stripe API key format for backend; expected key starting with 'sk_'.");
        }
        return RequestOptions.builder()
                .setApiKey(apiKey)
                .build();
    }

    private String resolveApiKey() {
        String secretKey = environment.getProperty("stripe.secret-key");
        if (secretKey != null && !secretKey.isBlank()) {
            return secretKey.trim();
        }
        String apiKey = environment.getProperty("stripe.api-key");
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }
        return null;
    }
}
