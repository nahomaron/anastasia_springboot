package com.anastasia.Anastasia_BackEnd.UnitTests.mappers;

import com.anastasia.Anastasia_BackEnd.modules.events.mappers.EventManagerMapperImpl;
import com.anastasia.Anastasia_BackEnd.modules.events.mappers.EventMapperImpl;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.*;
import com.anastasia.Anastasia_BackEnd.modules.events.mappers.EventManagerMapper;
import com.anastasia.Anastasia_BackEnd.modules.events.mappers.EventMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventVisibilityType;
import com.anastasia.Anastasia_BackEnd.modules.events.model.Repetition;
import com.anastasia.Anastasia_BackEnd.modules.events.model.requests.EventManagerDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventManagerEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleMappersUnitTest {

    private final TenantMapper tenantMapper = new TenantMapperImpl();
    private final ChurchMapper churchMapper = new ChurchMapperImpl();
    private final PriestMapper priestMapper = new PriestMapperImpl();
    private final UsersMapper usersMapper = new UsersMapperImpl();
    private final EventMapper eventMapper = new EventMapperImpl();
    private final EventManagerMapper eventManagerMapper = new EventManagerMapperImpl();

    @Test
    void tenantMapper_roundTrip_shouldPreserveCoreFields() {
        TenantDTO dto = TenantDTO.builder()
                .tenantType(TenantType.CHURCH)
                .subscriptionPlan(SubscriptionPlan.PREMIUM)
                .ownerName("St. Anastasia")
                .email("owner@example.com")
                .phoneNumber("+251900000000")
                .password("Password1!")
                .confirmPassword("Password1!")
                .build();

        TenantEntity entity = tenantMapper.tenantDTOToEntity(dto);
        assertThat(entity.getOwnerName()).isEqualTo("St. Anastasia");
        assertThat(entity.getPhoneNumber()).isEqualTo("+251900000000");
        assertThat(entity.getTenantType()).isEqualTo(TenantType.CHURCH);
        assertThat(entity.getSubscriptionPlan()).isEqualTo(SubscriptionPlan.PREMIUM);

        TenantDTO mappedBack = tenantMapper.tenantEntityToDTO(entity);
        assertThat(mappedBack.getOwnerName()).isEqualTo("St. Anastasia");
        assertThat(mappedBack.getPhoneNumber()).isEqualTo("+251900000000");
        assertThat(mappedBack.getTenantType()).isEqualTo(TenantType.CHURCH);
    }

    @Test
    void churchMapper_shouldMapAddressAndContactDetails() {
        ChurchEntity entity = ChurchEntity.builder()
                .churchName("St. Mary")
                .churchNameTigrinya("ቤተ ክርስቲያን ቅዱስት ማርያም")
                .diocese("Addis Diocese")
                .email("church@example.com")
                .phone("+251900000000")
                .denomination("Orthodox")
                .description("Historic parish")
                .usesOurServices(true)
                .address(Address.builder()
                        .addressLine1("123 Main")
                        .addressLine2("Suite 1")
                        .city("Addis Ababa")
                        .stateProvince("AA")
                        .country("Ethiopia")
                        .postalCode("12345")
                        .build())
                .gpsLocation("9.03,38.74")
                .instagram("instagram.com/stmary")
                .youtube("youtube.com/stmary")
                .facebook("facebook.com/stmary")
                .build();

        ChurchDTO dto = churchMapper.churchEntityToDTO(entity);
        assertThat(dto.getChurchName()).isEqualTo("St. Mary");
        assertThat(dto.getChurchNameTigrinya()).isEqualTo("ቤተ ክርስቲያን ቅዱስት ማርያም");
        assertThat(dto.getDiocese()).isEqualTo("Addis Diocese");
        assertThat(dto.getEmail()).isEqualTo("church@example.com");
        assertThat(dto.getAddress().getCity()).isEqualTo("Addis Ababa");
        assertThat(dto.getPhone()).isEqualTo("+251900000000");
        assertThat(dto.getDenomination()).isEqualTo("Orthodox");
        assertThat(dto.isUsesOurServices()).isTrue();
        assertThat(dto.getInstagram()).isEqualTo("instagram.com/stmary");
        assertThat(dto.getYoutube()).isEqualTo("youtube.com/stmary");
        assertThat(dto.getFacebook()).isEqualTo("facebook.com/stmary");

        ChurchEntity mappedBack = churchMapper.churchDTOToEntity(dto);
        assertThat(mappedBack.getChurchName()).isEqualTo("St. Mary");
        assertThat(mappedBack.getChurchNameTigrinya()).isEqualTo("ቤተ ክርስቲያን ቅዱስት ማርያም");
        assertThat(mappedBack.getEmail()).isEqualTo("church@example.com");
        assertThat(mappedBack.getPhone()).isEqualTo("+251900000000");
        assertThat(mappedBack.getInstagram()).isEqualTo("instagram.com/stmary");
    }

    @Test
    void priestMapper_shouldHandleLanguagesAndAddress() {
        PriestDTO dto = PriestDTO.builder()
                .firstName("Abba")
                .fatherName("Gebre")
                .grandFatherName("Selassie")
                .phoneNumber("+251911111111")
                .personalEmail("abba@example.com")
                .churchEmail("church@example.com")
                .birthdate("1980-01-01")
                .languages(Set.of("Amharic", "Geez"))
                .levelOfEducation("Masters")
                .address(Address.builder()
                        .addressLine1("Church Rd")
                        .addressLine2("Hall A")
                        .city("Gondar")
                        .stateProvince("Amhara")
                        .country("Ethiopia")
                        .postalCode("98765")
                        .build())
                .password("Password1!")
                .confirmPassword("Password1!")
                .build();

        PriestEntity entity = priestMapper.priestDTOToEntity(dto);
        assertThat(entity.getFirstName()).isEqualTo("Abba");
        assertThat(entity.getLanguages()).containsExactlyInAnyOrder("Amharic", "Geez");
        assertThat(entity.getAddress().getCity()).isEqualTo("Gondar");

        entity.setStatus(PriestStatus.ACTIVE);
        PriestDTO mappedBack = priestMapper.priestEntityToDTO(entity);
        assertThat(mappedBack.getFirstName()).isEqualTo("Abba");
        assertThat(mappedBack.getLanguages()).contains("Geez");
    }

    @Test
    void usersMapper_shouldMapUserCredentials() {
        UserDTO dto = UserDTO.builder()
                .fullName("Test User")
                .email("user@example.com")
                .password("Password1!")
                .confirmPassword("Password1!")
                .build();

        UserEntity entity = usersMapper.userDTOToUserEntity(dto);
        assertThat(entity.getFullName()).isEqualTo("Test User");
        assertThat(entity.getEmail()).isEqualTo("user@example.com");
        assertThat(entity.getPassword()).isEqualTo("Password1!");

        UserDTO mappedBack = usersMapper.userEntityToUserDTO(entity);
        assertThat(mappedBack.getFullName()).isEqualTo("Test User");
        assertThat(mappedBack.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void eventMapper_shouldMapCoreEventDetails() {
        EventDTO dto = EventDTO.builder()
                .title("Youth Retreat")
                .description("Weekend retreat")
                .date(LocalDate.of(2024, 10, 5))
                .location("Nazareth")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(14, 0))
                .visibility(EventVisibilityType.ALL)
                .repetition(Repetition.NONE)
                .build();

        EventEntity entity = eventMapper.eventDTOToEntity(dto);
        assertThat(entity.getTitle()).isEqualTo("Youth Retreat");
        assertThat(entity.getLocation()).isEqualTo("Nazareth");
        assertThat(entity.getVisibility()).isEqualTo(EventVisibilityType.ALL);

        EventDTO mappedBack = eventMapper.eventEntityToDTO(entity);
        assertThat(mappedBack.getTitle()).isEqualTo("Youth Retreat");
        assertThat(mappedBack.getLocation()).isEqualTo("Nazareth");
    }

    @Test
    void eventManagerMapper_shouldMapAuditFields() {
        EventManagerDTO dto = EventManagerDTO.builder()
                .eventId(1L)
                .userId(UUID.randomUUID())
                .role("ORGANIZER")
                .assignedAt(LocalDateTime.now())
                .build();

        EventManagerEntity entity = eventManagerMapper.eventManagerDTOToEntity(dto);
        assertThat(entity.getRole()).isEqualTo("ORGANIZER");
        assertThat(entity.getAssignedAt()).isEqualTo(dto.getAssignedAt());

        EventManagerDTO mappedBack = eventManagerMapper.eventManagerEntityToDTO(entity);
        assertThat(mappedBack.getRole()).isEqualTo("ORGANIZER");
    }
}
