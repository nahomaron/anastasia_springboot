package com.anastasia.Anastasia_BackEnd.modules.payments.stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
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
public class StripeClient {
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

        var options = RequestOptions.builder()
                .setIdempotencyKey("checkout:" + tenant + ":" + idempotencyKey)
                .build();

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

        var options = RequestOptions.builder()
                .setIdempotencyKey("subscription:" + tenant + ":" + idempotencyKey)
                .build();

        return Session.create(params, options);
    }
}
