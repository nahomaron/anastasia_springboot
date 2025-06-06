package com.anastasia.Anastasia_BackEnd;

import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.child.ChildDTO;
import com.anastasia.Anastasia_BackEnd.model.child.ChildEntity;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.common.Address;
import com.anastasia.Anastasia_BackEnd.model.group.GroupDTO;
import com.anastasia.Anastasia_BackEnd.model.group.GroupEntity;
import com.anastasia.Anastasia_BackEnd.model.member.MemberDTO;
import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.model.permission.Permission;
import com.anastasia.Anastasia_BackEnd.model.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.model.user.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

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



}
