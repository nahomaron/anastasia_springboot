package com.anastasia.Anastasia_BackEnd.UnitTests.json;

import com.anastasia.Anastasia_BackEnd.modules.events.model.EventDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventDtoContractTest {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    void writeValueAsString_serializesCanonicalDateTimesOnly() throws Exception {
        EventDTO dto = EventDTO.builder()
                .startAt(LocalDateTime.of(2026, 3, 12, 9, 5))
                .endAt(LocalDateTime.of(2026, 3, 12, 11, 30))
                .build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        assertThat(json.has("startAt")).isTrue();
        assertThat(json.has("endAt")).isTrue();
        assertThat(json.has("date")).isFalse();
        assertThat(json.has("endDate")).isFalse();
        assertThat(json.has("startTime")).isFalse();
        assertThat(json.has("endTime")).isFalse();
    }

    @Test
    void readValue_deserializesCanonicalDateTimes() throws Exception {
        String json = """
                {
                  "startAt": "2026-03-12T09:05:00",
                  "endAt": "2026-03-12T11:30:00"
                }
                """;

        EventDTO dto = objectMapper.readValue(json, EventDTO.class);

        assertThat(dto.getStartAt()).isEqualTo(LocalDateTime.of(2026, 3, 12, 9, 5));
        assertThat(dto.getEndAt()).isEqualTo(LocalDateTime.of(2026, 3, 12, 11, 30));
    }
}
