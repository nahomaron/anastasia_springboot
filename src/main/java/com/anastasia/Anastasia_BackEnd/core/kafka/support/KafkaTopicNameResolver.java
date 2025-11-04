package com.anastasia.Anastasia_BackEnd.core.kafka.support;

import com.anastasia.Anastasia_BackEnd.core.kafka.util.KafkaTopicNames;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.events.PaymentEventType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.RegistrationEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.anastasia.Anastasia_BackEnd.core.kafka.util.KafkaTopicNames.CHILD_REGISTERED;
import static com.anastasia.Anastasia_BackEnd.core.kafka.util.KafkaTopicNames.MEMBER_REGISTERED;

/**
 * Resolves the concrete topic names (including prefixes) for publishers and listeners.
 */
@Component
@RequiredArgsConstructor
public class KafkaTopicNameResolver {

    private final KafkaInfrastructureProperties properties;

    public String resolve(Enum<?> eventType) {
        if (eventType instanceof PaymentEventType pet) return resolvePaymentTopic(pet);
        if (eventType instanceof RegistrationEventType ret) return resolveRegistrationTopic(ret);
        return defaultTopic();
    }

    private String resolvePaymentTopic(PaymentEventType eventType) {
        return switch (eventType) {
            case PAYMENT_AUTHORIZED -> paymentsAuthorized();
            case PAYMENT_CAPTURED -> paymentsCaptured();
            default -> paymentsEvents();
        };
    }

    private String resolveRegistrationTopic(RegistrationEventType eventType) {
        return switch (eventType) {
            case MEMBER_REGISTERED -> memberRegistered();
            case CHILD_REGISTERED -> childRegistered();
            default -> defaultTopic();
        };
    }

    private String defaultTopic() {
        return properties.applyPrefix(KafkaTopicNames.GENERIC_EVENTS);
    }



    public String memberRegistered(){return properties.applyPrefix(MEMBER_REGISTERED);}
    public String childRegistered(){return properties.applyPrefix(CHILD_REGISTERED);}

    public String paymentsAuthorized() {
        return properties.applyPrefix(KafkaTopicNames.PAYMENTS_AUTHORIZED);
    }
    public String paymentsCaptured() {
        return properties.applyPrefix(KafkaTopicNames.PAYMENTS_CAPTURED);
    }
    public String paymentsEvents() {
        return properties.applyPrefix(KafkaTopicNames.PAYMENTS_EVENTS);
    }

    public String subscriptionsActivated() {
        return properties.applyPrefix(KafkaTopicNames.SUBSCRIPTIONS_ACTIVATED);
    }
    public String subscriptionsCanceled() {
        return properties.applyPrefix(KafkaTopicNames.SUBSCRIPTIONS_CANCELED);
    }

    public String deadLetterFor(String topic) {
        return properties.deadLetterTopic(topic);
    }
}
