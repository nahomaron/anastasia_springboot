package com.anastasia.Anastasia_BackEnd;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarType;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.groups.dto.GroupDTO;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.Permission;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestStatus;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TestDataUtil {

    private TestDataUtil(){}
    public static final String TEST_PASSWORD = "Password123!";

    public static UserEntity createTestUserEntityA(){
        return UserEntity.builder()
                .fullName("Gebray weldu")
                .email(uniqueEmail("gebray", "gmail.com"))
                .password(TEST_PASSWORD)
                .build();
    }

    public static UserDTO createTestUserDTO(){
        return UserDTO.builder()
                .fullName("Gebray weldu")
                .email(uniqueEmail("gebray", "gmail.com"))
                .password(TEST_PASSWORD)
                .confirmPassword(TEST_PASSWORD)
                .build();
    }

    public static AuthenticationRequest createTestAuthenticationRequest(String email) {
        return AuthenticationRequest.builder()
                .email(email)
                .password(TEST_PASSWORD)
                .build();
    }

    public static GroupDTO createTestGroupDTO(String churchId){
        return GroupDTO.builder()
                .churchId(churchId)
                .groupName("Integration Test Group " + uniqueSuffix())
                .description("Integration description")
                .visibility("public")
                .users(Collections.emptySet())
                .managers(Collections.emptySet())
                .build();
    }

    public static ChurchEntity createTestChurchEntity(TenantEntity tenant) {
        return ChurchEntity.builder()
                .prefix("St.")
                .churchName("St. Michael Church")
                .churchNameTigrinya("ቤተ ክርስቲያን ቅዱስ ሚካኤል")
                .churchNumber("M" + uniqueSuffix())
                .tenant(tenant)
                .email("stmichael+" + uniqueSuffix() + "@church.org")
                .diocese("North America")
                .phone("+1555" + randomDigits(7))
                .denomination("Orthodox")
                .description("Test parish community")
                .usesOurServices(true)
                .address(Address.builder()
                        .addressLine1("123 Test St")
                        .addressLine2("Suite 100")
                        .city("Alexandria")
                        .stateProvince("VA")
                        .country("USA")
                        .postalCode("22310")
                        .build())
                .gpsLocation("38.8048,-77.0469")
                .instagram("instagram.com/stmichael/" + uniqueSuffix())
                .youtube("youtube.com/stmichael/" + uniqueSuffix())
                .facebook("facebook.com/stmichael/" + uniqueSuffix())
                .build();
    }

    public static ChurchDTO createTestChurchDTO() {
        return ChurchDTO.builder()
                .prefix("St.")
                .churchName("St. Michael Church")
                .churchNameTigrinya("ቤተ ክርስቲያን ቅዱስ ሚካኤል")
                .email("stgebriel+" + uniqueSuffix() + "@church.org")
                .diocese("North America")
                .phone("+1555" + randomDigits(7))
                .denomination("Orthodox")
                .description("Primary test parish DTO")
                .usesOurServices(true)
                .address(Address.builder()
                        .addressLine1("200 Unity Blvd")
                        .addressLine2("Building A")
                        .city("Reston")
                        .stateProvince("VA")
                        .country("USA")
                        .postalCode("20190")
                        .build())
                .gpsLocation("38.9506,-77.4250")
                .instagram("instagram.com/stmichael/" + uniqueSuffix())
                .youtube("youtube.com/stmichael/" + uniqueSuffix())
                .facebook("facebook.com/stmichael/" + uniqueSuffix())
                .build();
    }
    public static ChurchDTO createTestChurchDTO_B() {
        return ChurchDTO.builder()
                .prefix("St.")
                .churchName("St. Mary Church")
                .churchNameTigrinya("ቤተ ክርስቲያን ቅዱስት ማርያም")
                .email("stmary+" + uniqueSuffix() + "@church.org")
                .diocese("North America")
                .phone("+1555" + randomDigits(7))
                .denomination("Catholic")
                .description("Secondary test parish DTO")
                .usesOurServices(false)
                .address(Address.builder()
                        .addressLine1("400 Hope Ave")
                        .addressLine2("Floor 2")
                        .city("Arlington")
                        .stateProvince("VA")
                        .country("USA")
                        .postalCode("22202")
                        .build())
                .gpsLocation("38.8577,-77.0510")
                .instagram("instagram.com/stmary/" + uniqueSuffix())
                .youtube("youtube.com/stmary/" + uniqueSuffix())
                .facebook("facebook.com/st.mary/" + uniqueSuffix())
                .build();
    }

    public static GroupEntity createTestGroupEntity(ChurchEntity church, UUID tenantId){

        return GroupEntity.builder()
//                .groupId(1L)
                .tenantId(tenantId)
                .church(church)
                .groupName("Integration Test Group " + uniqueSuffix())
                .description("Integration description")
                .visibility("public")
                .users(Collections.emptySet())
                .managers(Collections.emptySet())
                .build();
    }

    public static TenantEntity createTestTenantEntity() {
        TenantEntity tenant = TenantEntity.builder()
                .tenantType(TenantType.CHURCH) // or PRIEST
                .ownerName("St. Mary Church")
                .phoneNumber("+1555000111")
                .isActiveTenant(true)
                .build();
        tenant.assignSubscription(
                TenantSubscriptionEntity.builder()
                        .plan(SubscriptionPlan.PREMIUM)
                        .status(SubscriptionStatus.ACTIVE)
                        .provider(BillingProvider.MANUAL)
                        .build()
        );
        return tenant;
    }

    public static TenantDTO createTestTenantDTO(){
        ChurchDTO church = ChurchDTO.builder()
                .churchName("St. Mary Church")
                .churchNameTigrinya("ቤተ ክርስቲያን ቅዱስት ማርያም")
                .diocese("Addis Ababa")
                .email(uniqueEmail("church", "example.com"))
                .phone("+1555" + randomDigits(7))
                .build();

        return TenantDTO.builder()
                .tenantType(TenantType.CHURCH) // or PRIEST
                .ownerName("St. Mary Church")
                .phoneNumber("+1555" + randomDigits(7))
                .subscriptionPlan(SubscriptionPlan.PREMIUM) // or BASIC, PRO, etc.
                .password(TEST_PASSWORD)
                .confirmPassword(TEST_PASSWORD)
                .email(uniqueEmail("tenant.owner", "example.com"))
                .church(church)
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

    public static Adult_MemberEntity createTestMember(ChurchEntity church) {
        return Adult_MemberEntity.builder()
                .membershipNumber("MBR-" + uniqueSuffix())
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
                .email(uniqueEmail("gebray.member", "gmail.com"))
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
                .termsAccepted(true)
                .termsVersion("test-v1")
                .termsAcceptedAt(Instant.now())
                .build();
    }

    public static Adult_MemberDTO createTestMemberDTO(ChurchEntity church) {
        return Adult_MemberDTO.builder()
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
                .avatar(AvatarDTO.builder()
                        .imageUrl("https://example.com/avatars/" + uniqueSuffix() + ".jpg")
                        .build())
                .phone("+1234567890")
                .maritalStatus("Single")
                .fatherOfConfession("Abba Abraham")
                .email(uniqueEmail("gebray.member", "gmail.com"))
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
                .termsAccepted(true)
                .termsVersion("test-v1")
                .termsAcceptedAt(Instant.now())
                .build();
    }

    public static Child_MemberEntity createTestChild(ChurchEntity church) {
        return Child_MemberEntity.builder()
                .membershipNumber("CHD-" + uniqueSuffix())
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
                .email(uniqueEmail("yonas.child", "gmail.com"))
                .phone("+1234500012")
                .whatsApp("+1234500012")
                .emergencyContactNumber("+1987612345")
                .contactRelation("Father")
                .firstLanguage("Tigrinya")
                .secondLanguage("English")
                .levelOfEducation("Grade 2")
                .fatherOfConfession("Abba Mikael")
                .address(Address.builder()
                        .addressLine1("123 School Street")
                        .addressLine2("Apt 5")
                        .city("Keren")
                        .country("Eritrea")
                        .stateProvince("Anseba")
                        .postalCode("7123")
                        .build())
                .build();
    }

    public static Child_MemberDTO createTestChildDTO(ChurchEntity church) {
        return Child_MemberDTO.builder()
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
                .email(uniqueEmail("yonas.child", "gmail.com"))
                .phone("+1234500012")
                .whatsApp("+1234500012")
                .emergencyContactNumber("+1987612345")
                .contactRelation("Father")
                .firstLanguage("Tigrinya")
                .secondLanguage("English")
                .levelOfEducation("Grade 2")
                .fatherOfConfession("Abba Mikael")
                .address(Address.builder()
                        .addressLine1("123 School Street")
                        .addressLine2("Apt 5")
                        .city("Keren")
                        .country("Eritrea")
                        .stateProvince("Anseba")
                        .postalCode("7123")
                        .build())
                .build();
    }

    public static PriestDTO createTestPriestDTO(String churchNumber) {
        return PriestDTO.builder()
                .churchNumber(churchNumber) // or provide a valid church number if needed
                .tenantId(null)     // or UUID.randomUUID() for tenant priest
                .avatar(AvatarDTO.builder()
                        .imageUrl("https://example.com/photo.jpg")
                        .imageSize("original")
                        .build())
                .prefixes("Abba")
                .firstName("Dawit")
                .fatherName("Tekle")
                .grandFatherName("Berhane")
                .phoneNumber(uniquePhoneNumber("+2519"))
                .personalEmail("abba.dawit" + UUID.randomUUID() + "@mail.com")
                .churchEmail("church.contact@mail.com")
                .priesthoodCardId("PR-2025-XYZ" + uniqueSuffix())
                .priesthoodCardScan("https://example.com/card-scan.png")
                .birthdate("1990-04-15")
                .languages(Set.of("Tigrigna", "Amharic", "English"))
                .levelOfEducation("Master of Divinity")
                .address(Address.builder()
                        .addressLine1("123 Abune Tekle Street")
                        .addressLine2("House 9")
                        .city("Asmara")
                        .stateProvince("Zoba Maekel")
                        .country("Eritrea")
                        .postalCode("0000")
                        .build())
                .password("StrongP@ss1")
                .confirmPassword("StrongP@ss1")
                .build();
    }

    public static PriestDTO createTestPriestDTO_B(String churchNumber) {
        return PriestDTO.builder()
                .churchNumber(churchNumber)
                .tenantId(null)
                .avatar(AvatarDTO.builder()
                        .imageUrl("https://example.com/images/priest_b.jpg")
                        .imageSize("original")
                        .build())
                .prefixes("Keshi")
                .firstName("Michael")
                .fatherName("Abraham")
                .grandFatherName("Hagos")
                .phoneNumber(uniquePhoneNumber("+2519"))
                .personalEmail("keshi.michael" + UUID.randomUUID() + "@mail.com")
                .churchEmail("michael.church@church.org")
                .priesthoodCardId("PRT-1002" + uniqueSuffix())
                .priesthoodCardScan("https://example.com/docs/priest_card_b.pdf")
                .birthdate("1972-04-18")
                .languages(Set.of("Amharic", "English"))
                .levelOfEducation("Master of Divinity")
                .address(Address.builder()
                        .addressLine1("Divine Way 21")
                        .addressLine2("Unit B")
                        .city("Addis Ababa")
                        .stateProvince("Addis Ababa")
                        .country("Ethiopia")
                        .postalCode("2000")
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
                .avatar(AvatarEntity.builder()
                        .ownerId(UUID.randomUUID())
                        .avatarType(AvatarType.USER)
                        .imageUrl("https://example.com/priest/profile.jpg")
                        .imageSize("original")
                        .build())
                .prefixes("Abba")
                .firstName("Yohannes")
                .fatherName("Tesfay")
                .grandFatherName("Kifle")
                .phoneNumber(uniquePhoneNumber("+2519"))
                .churchEmail("abba.yohannes@church.org")
                .priesthoodCardId("PCID-2025-003" + uniqueSuffix())
                .priesthoodCardScan("https://example.com/scans/priest_card.png")
                .birthdate("1985-08-25")
                .languages(Set.of("Geez", "Tigrigna", "English"))
                .levelOfEducation("Bachelor of Theology")
                .address(Address.builder()
                        .addressLine1("Church Street 42")
                        .addressLine2("Parish House")
                        .city("Mekelle")
                        .stateProvince("Tigray")
                        .country("Ethiopia")
                        .postalCode("1000")
                        .build())
                .spiritualChildren(0)
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
                .email(uniqueEmail("gebray.user", "gmail.com"))
                .password(TEST_PASSWORD)
                .roles(new HashSet<>(Set.of(savedRole)))
                .build();
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private static String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private static String uniqueEmail(String prefix, String domain) {
        return prefix + "." + uniqueSuffix() + "@" + domain;
    }

    private static String uniquePhoneNumber(String prefix) {
        return prefix + randomDigits(8);
    }

    private static String randomDigits(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }
}
