package com.anastasia.Anastasia_BackEnd.UnitTests.mappers;

import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.MemberMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.EducationLevel;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberGender;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberLifecycleStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MemberMapperUnitTest {

    private final MemberMapper mapper = new MemberMapper();

    @Test
    void memberEntityToDTO_shouldMapAllRelevantFields() {
        Adult_MemberEntity entity = Adult_MemberEntity.builder()
                .churchNumber("CH12345")
                .statusValue(MemberLifecycleStatus.ACTIVE)
                .approvedByChurch(true)
                .approvedByPriest(false)
                .deacon(true)
                .title("Mr")
                .firstName("John")
                .fatherName("Doe")
                .grandFatherName("Senior")
                .motherName("Jane")
                .mothersFather("Grandpa")
                .firstNameT("ሃና")
                .fatherNameT("ምንያም")
                .grandFatherNameT("እስጢፋኖስ")
                .motherFullNameT("ሐና ስለም")
                .genderValue(MemberGender.MALE)
                .birthday(LocalDate.of(1990, 1, 1))
                .nationality("Ethiopian")
                .placeOfBirth("Addis Ababa")
                .email("john@example.com")
                .phone("+251900000000")
                .whatsApp("+251911111111")
                .emergencyContactNumber("+251922222222")
                .contactRelation("Brother")
                .eritreaContact("+251933333333")
                .maritalStatus("Single")
                .numberOfChildren(0)
                .firstLanguage("Amharic")
                .secondLanguage("English")
                .profession("Engineer")
                .educationLevelValue(EducationLevel.MASTERS)
                .fatherOfConfession("Father Abraham")
                .spouseIdNumber("SP1234")
                .termsAccepted(true)
                .termsVersion("test-v1")
                .termsAcceptedAt(Instant.now())
                .address(Address.builder()
                        .addressLine1("123 Main St")
                        .addressLine2("Unit 7")
                        .city("Addis")
                        .stateProvince("AA")
                        .country("Ethiopia")
                        .postalCode("12345")
                        .build())
                .build();

        Adult_MemberDTO dto = mapper.memberEntityToDTO(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getChurchNumber()).isEqualTo("CH12345");
        assertThat(dto.getFirstName()).isEqualTo("John");
        assertThat(dto.getMotherName()).isEqualTo("Jane");
        assertThat(dto.getProfession()).isEqualTo("Engineer");
        assertThat(dto.getAddress().getCity()).isEqualTo("Addis");
    }

    @Test
    void memberEntityToDTO_withNull_returnsNull() {
        assertThat(mapper.memberEntityToDTO(null)).isNull();
    }

    @Test
    void memberDTOToEntity_shouldMapBackToEntity() {
        Adult_MemberDTO dto = Adult_MemberDTO.builder()
                .churchNumber("CH54321")
                .deacon(false)
                .title("Mrs")
                .firstName("Hanna")
                .fatherName("Kidane")
                .grandFatherName("Tsegay")
                .motherName("Sara")
                .mothersFather("Berhane")
                .firstNameT("ሐና")
                .fatherNameT("ቅዳኔ")
                .grandFatherNameT("ጸጋይ")
                .motherFullNameT("ሳራ በርሀነ")
                .gender("Female")
                .birthday(LocalDate.of(1995, 5, 10))
                .nationality("Eritrean")
                .placeOfBirth("Asmara")
                .email("hanna@example.com")
                .phone("+2917000000")
                .whatsApp("+2917111111")
                .emergencyContactNumber("+2917222222")
                .contactRelation("Sister")
                .eritreaContact("+2917333333")
                .maritalStatus("Married")
                .numberOfChildren(2)
                .firstLanguage("Tigrinya")
                .secondLanguage("Italian")
                .profession("Designer")
                .levelOfEducation("Bachelors")
                .fatherOfConfession("Father Yohannes")
                .spouseIdNumber("SP9999")
                .termsAccepted(true)
                .termsVersion("test-v1")
                .termsAcceptedAt(Instant.now())
                .address(Address.builder()
                        .addressLine1("456 Side Rd")
                        .addressLine2("Block C")
                        .city("Asmara")
                        .stateProvince("Central")
                        .country("Eritrea")
                        .postalCode("54321")
                        .build())
                .build();

        Adult_MemberEntity entity = mapper.memberDTOToEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getChurchNumber()).isEqualTo("CH54321");
        assertThat(entity.getFirstName()).isEqualTo("Hanna");
        assertThat(entity.getProfession()).isEqualTo("Designer");
        assertThat(entity.getAddress().getCity()).isEqualTo("Asmara");
    }

    @Test
    void memberDTOToEntity_withNull_returnsNull() {
        assertThat(mapper.memberDTOToEntity(null)).isNull();
    }
}
