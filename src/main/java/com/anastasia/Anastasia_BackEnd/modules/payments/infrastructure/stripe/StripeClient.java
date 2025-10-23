package com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.stripe;

import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripeClient {
    @Value("${stripe.success-url}") private String successUrlTemplate;
    @Value("${stripe.cancel-url}")  private String cancelUrlTemplate;

    public Session createCheckoutSession(String paymentId,
                                         long amountMinor,
                                         String currency,
                                         String purposeLabel) throws Exception {

        String successUrl = successUrlTemplate.replace("{PAYMENT_ID}", paymentId);
        String cancelUrl  = cancelUrlTemplate.replace("{PAYMENT_ID}", paymentId);

        var params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currency.toLowerCase())
                                .setUnitAmount(amountMinor)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Anastasia: " + purposeLabel).build())
                                .build())
                        .build())
                .putMetadata("paymentId", paymentId)
                .build();

        return Session.create(params);
    }
}
