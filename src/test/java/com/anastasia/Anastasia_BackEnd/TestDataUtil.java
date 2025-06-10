package com.anastasia.Anastasia_BackEnd;

import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.child.ChildDTO;
import com.anastasia.Anastasia_BackEnd.model.child.ChildEntity;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.common.Address;
import com.anastasia.Anastasia_BackEnd.model.group.GroupDTO;
import com.anastasia.Anastasia_BackEnd.model.group.GroupEntity;
import com.anastasia.Anastasia_BackEnd.model.member.MemberDTO;
import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.model.permission.Permission;
import com.anastasia.Anastasia_BackEnd.model.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestDTO;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestStatus;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.model.user.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.auth.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.RoleRepository;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TestDataUtil {

    private TestDataUtil(){}
    public static final String TEST_PASSWORD = "Password123!";

    public static UserEntity createTestUserEntityA(){
        return UserEntity.builder()
                .fullName("Gebray weldu")
                .email("gebray@gmail.com")
                .password(TEST_PASSWORD)
                .build();
    }

    public static UserDTO createTestUserDTO(){
        return UserDTO.builder()
                .fullName("Gebray weldu")
                .email("gebray@gmail.com")
                .password(TEST_PASSWORD)
                .confirmPassword(TEST_PASSWORD)
                .build();
    }

    public static AuthenticationRequest createTestAuthenticationRequest() {
        return AuthenticationRequest.builder()
                .email("gebray@gmail.com")
                .password(TEST_PASSWORD)
                .build();
    }

    public static GroupDTO createTestGroupDTO(String churchId){
        return GroupDTO.builder()
                .churchId(churchId)
                .groupName("Integration Test Group")
                .description("Integration description")
                .visibility("public")
                .users(Collections.emptySet())
                .managers(Collections.emptySet())
                .build();
    }

    public static ChurchEntity createTestChurchEntity(TenantEntity tenant) {
        return ChurchEntity.builder()
                .churchName("St. Michael Church")
                .churchNumber("M123")
                .tenant(tenant)
                .email("stmichael@church.org")
                .diocese("North America")
                .facebookPage("facebook.com/stmichael")
                .build();
    }

    public static ChurchDTO createTestChurchDTO() {
        return ChurchDTO.builder()
                .churchName("St. Michael Church")
                .email("stgebriel@church.org")
                .diocese("North America")
                .facebookPage("facebook.com/stmichael")
                .build();
    }
    public static ChurchDTO createTestChurchDTO_B() {
        return ChurchDTO.builder()
                .churchName("St. Mary Church")
                .email("stmary@church.org")
                .diocese("North America")
                .facebookPage("facebook.com/st.mary")
                .build();
    }

    public static GroupEntity createTestGroupEntity(ChurchEntity church, UUID tenantId){

        return GroupEntity.builder()
//                .groupId(1L)
                .tenantId(tenantId)
                .church(church)
                .groupName("Integration Test Group")
                .description("Integration description")
                .visibility("public")
                .users(Collections.emptySet())
                .managers(Collections.emptySet())
                .build();
    }

    public static TenantEntity createTestTenantEntity() {
        return TenantEntity.builder()
                .tenantType(TenantType.CHURCH) // or PRIEST
                .ownerName("St. Mary Church")
                .phoneNumber("+1555000111")
                .subscriptionPlan(SubscriptionPlan.PREMIUM) // or BASIC, PRO, etc.
                .isActiveTenant(true)
                .isPaymentConfirmed(true)
                .build();
    }

    public static TenantDTO createTestTenantDTO(){
        return TenantDTO.builder()
                .tenantType(TenantType.CHURCH) // or PRIEST
                .ownerName("St. Mary Church")
                .phoneNumber("+1555000111")
                .subscriptionPlan(SubscriptionPlan.PREMIUM) // or BASIC, PRO, etc.
                .password(TEST_PASSWORD)
                .confirmPassword(TEST_PASSWORD)
                .email("welday@gmail.com")
                .build();
    }

    public static Role createTestOwnerRole(TenantEntity tenant){
        return Role.builder()
                .description("Owns the subscription")
                .tenant(tenant)
                .permissions(Set.of(Permission.builder()
                                .name(PermissionType.OWN_SUBSCRIPTION)
                                .description("All permissions")
                        .build()))
                .roleName("Owner")
                .build();
    }

    public static MemberEntity createTestMember(ChurchEntity church) {
        return MemberEntity.builder()
                .membershipNumber("MBR-001")
                .church(church)
                .churchNumber(church.getChurchNumber())
                .status("ACTIVE")
                .approvedByChurch(true)
                .approvedByPriest(true)
                .deacon(false)
                .title("Mr.")
                .firstName("Nahom")
                .fatherName("Aron")
                .grandFatherName("Dawit")
                .motherName("Ruth")
                .mothersFather("Yohannes")
                .firstNameT("ናሆም")
                .fatherNameT("ኣሮን")
                .grandFatherNameT("ዳዊት")
                .motherFullNameT("ሩት ዮሓንስ")
                .gender("Male")
                .birthday(LocalDate.of(1990, Month.DECEMBER, 3))
                .phone("+1234567890")
                .maritalStatus("Single")
                .fatherOfConfession("Abba Abraham")
                .email("gebray@gmail.com")
                .nationality("Eritrean")
                .placeOfBirth("Asmara")
                .whatsApp("+1234567890")
                .emergencyContactNumber("+1987654321")
                .contactRelation("Brother")
                .eritreaContact("021000000")
                .numberOfChildren(0)
                .firstLanguage("Tigrinya")
                .secondLanguage("English")
                .profession("Engineer")
                .levelOfEducation("BSc")
                .build();
    }

    public static MemberDTO createTestMemberDTO(ChurchEntity church) {
        return MemberDTO.builder()
                .churchNumber(church.getChurchNumber())
                .deacon(false)
                .title("Mr.")
                .firstName("Nahom")
                .fatherName("Aron")
                .grandFatherName("Dawit")
                .motherName("Ruth")
                .mothersFather("Yohannes")
                .firstNameT("ናሆም")
                .fatherNameT("ኣሮን")
                .grandFatherNameT("ዳዊት")
                .motherFullNameT("ሩት ዮሓንስ")
                .gender("Male")
                .birthday(LocalDate.of(1990, Month.DECEMBER, 3))
                .phone("+1234567890")
                .maritalStatus("Single")
                .fatherOfConfession("Abba Abraham")
                .email("gebray@gmail.com")
                .nationality("Eritrean")
                .placeOfBirth("Asmara")
                .whatsApp("+1234567890")
                .emergencyContactNumber("+1987654321")
                .contactRelation("Brother")
                .eritreaContact("021000000")
                .numberOfChildren(0)
                .firstLanguage("Tigrinya")
                .secondLanguage("English")
                .profession("Engineer")
                .levelOfEducation("BSc")
                .build();
    }

    public static ChildEntity createTestChild(ChurchEntity church) {
        return ChildEntity.builder()
                .membershipNumber("CHD-001")
                .church(church)
                .churchNumber(church.getChurchNumber())
                .status("ACTIVE")
                .deacon(false)
                .title("Master")
                .firstName("Yonas")
                .fatherName("Samuel")
                .grandFatherName("Bereket")
                .motherName("Martha")
                .mothersFather("Tesfaye")
                .firstNameT("ዮናስ")
                .fatherNameT("ሳሙኤል")
                .grandFatherNameT("በረከት")
                .motherFullNameT("ማርታ ተስፋዬ")
                .gender("Male")
                .birthday(LocalDate.of(2015, Month.MARCH, 15))
                .nationality("Eritrean")
                .placeOfBirth("Keren")
                .email("yonas.child@gmail.com")
                .phone("+1234500012")
                .whatsApp("+1234500012")
                .emergencyContactNumber("+1987612345")
                .contactRelation("Father")
                .firstLanguage("Tigrinya")
                .secondLanguage("English")
                .levelOfEducation("Grade 2")
                .fatherOfConfession("Abba Mikael")
                .address(Address.builder()
                        .city("Keren")
                        .country("Eritrea")
                        .province("Anseba")
                        .street("123 School Street")
                        .zipcode("7123")
                        .build())
                .build();
    }

    public static ChildDTO createTestChildDTO(ChurchEntity church) {
        return ChildDTO.builder()
                .churchNumber(church.getChurchNumber())
                .deacon(false)
                .title("Master")
                .firstName("Yonas")
                .fatherName("Samuel")
                .grandFatherName("Bereket")
                .motherName("Martha")
                .mothersFather("Tesfaye")
                .firstNameT("ዮናስ")
                .fatherNameT("ሳሙኤል")
                .grandFatherNameT("በረከት")
                .motherFullNameT("ማርታ ተስፋዬ")
                .gender("Male")
                .birthday(LocalDate.of(2015, Month.MARCH, 15))
                .nationality("Eritrean")
                .placeOfBirth("Keren")
                .email("yonas.child@gmail.com")
                .phone("+1234500012")
                .whatsApp("+1234500012")
                .emergencyContactNumber("+1987612345")
                .contactRelation("Father")
                .firstLanguage("Tigrinya")
                .secondLanguage("English")
                .levelOfEducation("Grade 2")
                .fatherOfConfession("Abba Mikael")
                .address(Address.builder()
                        .city("Keren")
                        .country("Eritrea")
                        .province("Anseba")
                        .street("123 School Street")
                        .zipcode("7123")
                        .build())
                .build();
    }

    public static PriestDTO createTestPriestDTO(String churchNumber) {
        return PriestDTO.builder()
                .churchNumber(churchNumber) // or provide a valid church number if needed
                .tenantId(null)     // or UUID.randomUUID() for tenant priest
                .profilePicture("https://example.com/photo.jpg")
                .prefixes("Abba")
                .firstName("Dawit")
                .fatherName("Tekle")
                .grandFatherName("Berhane")
                .phoneNumber("+251911223344")
                .personalEmail("abba.dawit" + UUID.randomUUID() + "@mail.com")
                .churchEmail("church.contact@mail.com")
                .priesthoodCardId("PR-2025-XYZ")
                .priesthoodCardScan("https://example.com/card-scan.png")
                .birthdate("1990-04-15")
                .languages(Set.of("Tigrigna", "Amharic", "English"))
                .levelOfEducation("Master of Divinity")
                .address(Address.builder()
                        .street("123 Abune Tekle Street")
                        .city("Asmara")
                        .province("Zoba Maekel")
                        .country("Eritrea")
                        .zipcode("0000")
                        .build())
                .password("StrongP@ss1")
                .confirmPassword("StrongP@ss1")
                .build();
    }

    public static PriestDTO createTestPriestDTO_B(String churchNumber) {
        return PriestDTO.builder()
                .churchNumber(churchNumber)
                .tenantId(null)
                .profilePicture("https://example.com/images/priest_b.jpg")
                .prefixes("Keshi")
                .firstName("Michael")
                .fatherName("Abraham")
                .grandFatherName("Hagos")
                .phoneNumber("+251911778899")
                .personalEmail("keshi.michael@example.com")
                .churchEmail("michael.church@church.org")
                .priesthoodCardId("PRT-1002")
                .priesthoodCardScan("https://example.com/docs/priest_card_b.pdf")
                .birthdate("1972-04-18")
                .languages(Set.of("Amharic", "English"))
                .levelOfEducation("Master of Divinity")
                .address(Address.builder()
                        .street("Divine Way 21")
                        .city("Addis Ababa")
                        .province("Addis Ababa")
                        .country("Ethiopia")
                        .zipcode("2000")
                        .build())
                .password("StrongP@ssword2")
                .confirmPassword("StrongP@ssword2")
                .build();
    }


    public static PriestEntity createTestPriestEntity(ChurchEntity church, TenantEntity tenant) {
        return PriestEntity.builder()
                .priestNumber("PR" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .church(church)
                .churchNumber(church != null ? church.getChurchNumber() : null)
                .tenant(tenant)
                .status(PriestStatus.ACTIVE)
                .profilePicture("https://example.com/priest/profile.jpg")
                .prefixes("Abba")
                .firstName("Yohannes")
                .fatherName("Tesfay")
                .grandFatherName("Kifle")
                .phoneNumber("+251911334455")
                .churchEmail("abba.yohannes@church.org")
                .priesthoodCardId("PCID-2025-003")
                .priesthoodCardScan("https://example.com/scans/priest_card.png")
                .birthdate("1985-08-25")
                .languages(Set.of("Geez", "Tigrigna", "English"))
                .levelOfEducation("Bachelor of Theology")
                .address(Address.builder()
                        .street("Church Street 42")
                        .city("Mekelle")
                        .province("Tigray")
                        .country("Ethiopia")
                        .zipcode("1000")
                        .build())
                .isActive(true)
                .build();
    }


    public static UserEntity createTestUserWithPermissions(
            Set<PermissionType> permissionTypes,
            TenantEntity tenant,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository
    ) {
        // Fetch permissions from DB
        Set<Permission> permissions = permissionTypes.stream()
                .map(pt -> permissionRepository.findByName(pt)
                        .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + pt.name())))
                .collect(Collectors.toSet());

        // Create a dynamic role
        Role testRole = Role.builder()
                .roleName("TEST_ROLE_" + UUID.randomUUID())
                .description("Auto-generated test role for integration test")
                .permissions(permissions)
                .tenant(tenant)
                .tenantId(tenant.getId())
                .build();
        Role savedRole = roleRepository.save(testRole);

        // Prepare user with role assigned (not saved!)
        return UserEntity.builder()
                .fullName("Test User")
                .email("gebray@gmail.com")
                .password(TEST_PASSWORD)
                .roles(Set.of(savedRole))
                .build();
    }


}
