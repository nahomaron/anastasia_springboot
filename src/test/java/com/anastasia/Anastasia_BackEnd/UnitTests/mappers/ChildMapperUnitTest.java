package com.anastasia.Anastasia_BackEnd.UnitTests.mappers;


import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.ChildMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ChildServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ChildMapperUnitTest {

    private ChildServiceImpl childService;

    @BeforeEach
    void setUp() {
        childService = new ChildServiceImpl(
                null, // childRepository not needed
                null, // churchRepository not needed
                null, // userRepository not needed
                new ChildMapper(), // use real mapper
                null  // securityUtils not needed
        );
    }

    @Test
    void testConvertToDTO_shouldMapCorrectly() {
        Child_MemberEntity entity = Child_MemberEntity.builder()
                .churchNumber("CH001")
                .deacon(true)
                .title("Mr.")
                .firstName("John")
                .fatherName("Doe")
                .grandFatherName("Smith")
                .email("john@example.com")
                .phone("123456789")
                .build();

        Child_MemberDTO dto = childService.convertToDTO(entity);

        assertNotNull(dto);
        assertEquals("CH001", dto.getChurchNumber());
        assertEquals(true, dto.isDeacon());
        assertEquals("John", dto.getFirstName());
        assertEquals("Doe", dto.getFatherName());
        assertEquals("Smith", dto.getGrandFatherName());
        assertEquals("john@example.com", dto.getEmail());
    }

    @Test
    void testConvertToEntity_shouldMapCorrectly() {
        Child_MemberDTO dto = Child_MemberDTO.builder()
                .churchNumber("CH002")
                .deacon(false)
                .title("Miss")
                .firstName("Anna")
                .fatherName("Mekonnen")
                .grandFatherName("Tsegay")
                .email("anna@example.com")
                .phone("987654321")
                .build();

        Child_MemberEntity entity = childService.convertToEntity(dto);

        assertNotNull(entity);
        assertEquals("CH002", entity.getChurchNumber());
        assertEquals(false, entity.isDeacon());
        assertEquals("Anna", entity.getFirstName());
        assertEquals("Mekonnen", entity.getFatherName());
        assertEquals("Tsegay", entity.getGrandFatherName());
        assertEquals("anna@example.com", entity.getEmail());
    }

    @Test
    void testConvertToDTO_withNullEntity_shouldReturnNull() {
        assertNull(childService.convertToDTO(null));
    }

    @Test
    void testConvertToEntity_withNullDTO_shouldReturnNull() {
        assertNull(childService.convertToEntity(null));
    }
}

