package com.anastasia.Anastasia_BackEnd.core.auth.service;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OAuthLoginTicketService {

    private static final Duration TICKET_TTL = Duration.ofMinutes(5);

    private final Map<String, TicketEntry> tickets = new ConcurrentHashMap<>();
    private final LocalizedMessageService messageService;

    public String store(AuthenticationResponse response) {
        cleanupExpiredTickets();

        String ticket = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        tickets.put(ticket, new TicketEntry(response, Instant.now().plus(TICKET_TTL)));
        return ticket;
    }

    public AuthenticationResponse consume(String ticket) {
        cleanupExpiredTickets();

        TicketEntry entry = tickets.remove(ticket);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException(messageService.get("auth.oauth.ticketInvalid", "OAuth login ticket is invalid or expired."));
        }
        return entry.response();
    }

    private void cleanupExpiredTickets() {
        Instant now = Instant.now();
        tickets.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record TicketEntry(AuthenticationResponse response, Instant expiresAt) {
    }
}
