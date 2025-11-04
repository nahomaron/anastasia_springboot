package com.anastasia.Anastasia_BackEnd.UnitTests.specification;

import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.common.specification.MemberSpecifications;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MemberSpecificationTest {

    @SuppressWarnings("unchecked")
    @Test
    void testHasMembershipNumber() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<Adult_MemberEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Path<Long> membershipNumberPath = mock(Path.class);
        Predicate expectedPredicate = mock(Predicate.class);

        // 👇 FIX: Force the generic type so Mockito understands it
        Mockito.<Path<Long>>when(root.get("membershipNumber")).thenReturn(membershipNumberPath);
        when(cb.equal(membershipNumberPath, 123L)).thenReturn(expectedPredicate);

        Specification<Adult_MemberEntity> spec = MemberSpecifications.hasMembershipNumber(123L);
        Predicate actualPredicate = spec.toPredicate(root, query, cb);

        assertEquals(expectedPredicate, actualPredicate);
    }


    @SuppressWarnings("unchecked")
    @Test
    void testHasStatus() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<Adult_MemberEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Path<String> path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        Mockito.<Path<String>>when(root.get("status")).thenReturn(path);
        when(cb.equal(path, "ACTIVE")).thenReturn(predicate);

        Specification<Adult_MemberEntity> spec = MemberSpecifications.hasStatus("ACTIVE");
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(predicate, result);
    }

    @Test
    void testIsDeacon() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<Adult_MemberEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Path<Boolean> path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        Mockito.<Path<Boolean>>when(root.get("deacon")).thenReturn(path);
        when(cb.equal(path, true)).thenReturn(predicate);

        Specification<Adult_MemberEntity> spec = MemberSpecifications.isDeacon(true);
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(predicate, result);
    }

    @Test
    void testNameContains() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<Adult_MemberEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);

        // Path mocks
        Path<String> firstName = mock(Path.class);
        Path<String> fatherName = mock(Path.class);
        Path<String> grandFatherName = mock(Path.class);
        Path<String> firstNameT = mock(Path.class);
        Path<String> fatherNameT = mock(Path.class);
        Path<String> grandFatherNameT = mock(Path.class);

        // Expression mocks after cb.lower(...)
        Expression<String> expr1 = mock(Expression.class);
        Expression<String> expr2 = mock(Expression.class);
        Expression<String> expr3 = mock(Expression.class);

        // Predicate mocks
        Predicate p1 = mock(Predicate.class);
        Predicate p2 = mock(Predicate.class);
        Predicate p3 = mock(Predicate.class);
        Predicate p4 = mock(Predicate.class);
        Predicate p5 = mock(Predicate.class);
        Predicate p6 = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);

        // Explicitly define generic type for Path mocks
        Mockito.<Path<String>>when(root.get("firstName")).thenReturn(firstName);
        Mockito.<Path<String>>when(root.get("fatherName")).thenReturn(fatherName);
        Mockito.<Path<String>>when(root.get("grandFatherName")).thenReturn(grandFatherName);
        Mockito.<Path<String>>when(root.get("firstNameT")).thenReturn(firstNameT);
        Mockito.<Path<String>>when(root.get("fatherNameT")).thenReturn(fatherNameT);
        Mockito.<Path<String>>when(root.get("grandFatherNameT")).thenReturn(grandFatherNameT);

        String pattern = "%john%";

        when(cb.lower(firstName)).thenReturn(expr1);
        when(cb.lower(fatherName)).thenReturn(expr2);
        when(cb.lower(grandFatherName)).thenReturn(expr3);

        when(cb.like(expr1, pattern)).thenReturn(p1);
        when(cb.like(expr2, pattern)).thenReturn(p2);
        when(cb.like(expr3, pattern)).thenReturn(p3);
        when(cb.like(firstNameT, pattern)).thenReturn(p4);
        when(cb.like(fatherNameT, pattern)).thenReturn(p5);
        when(cb.like(grandFatherNameT, pattern)).thenReturn(p6);

        when(cb.or(p1, p2, p3, p4, p5, p6)).thenReturn(finalPredicate);

        Specification<Adult_MemberEntity> spec = MemberSpecifications.nameContains("john");
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(finalPredicate, result);
    }



    @Test
    void testAgeBetween() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Adult_MemberEntity> root = mock(Root.class);
        Path<LocalDate> birthdayPath = mock(Path.class);
        Predicate expectedPredicate = mock(Predicate.class);

        Mockito.<Path<LocalDate>>when(root.get("birthday")).thenReturn(birthdayPath);

        LocalDate today = LocalDate.now();
        LocalDate minDate = today.minusYears(50); // oldest
        LocalDate maxDate = today.minusYears(20); // youngest

        when(cb.between(birthdayPath, minDate, maxDate)).thenReturn(expectedPredicate);

        Specification<Adult_MemberEntity> spec = MemberSpecifications.ageBetween(20, 50);
        Predicate actualPredicate = spec.toPredicate(root, query, cb);

        assertEquals(expectedPredicate, actualPredicate);
    }

    @Test
    void testHasMaritalStatus() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Adult_MemberEntity> root = mock(Root.class);
        Path<String> maritalStatusPath = mock(Path.class);
        Predicate expected = mock(Predicate.class);

        Mockito.<Path<String>>when(root.get("maritalStatus")).thenReturn(maritalStatusPath);
        when(cb.equal(maritalStatusPath, "Married")).thenReturn(expected);

        Specification<Adult_MemberEntity> spec = MemberSpecifications.hasMaritalStatus("Married");
        Predicate actual = spec.toPredicate(root, query, cb);

        assertEquals(expected, actual);
    }

    @Test
    void testPhoneContains_shouldSearchPhoneAndWhatsapp() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Adult_MemberEntity> root = mock(Root.class);

        Path<String> phonePath = mock(Path.class);
        Path<String> whatsappPath = mock(Path.class);

        Mockito.<Path<String>>when(root.get("phone")).thenReturn(phonePath);
        Mockito.<Path<String>>when(root.get("whatsApp")).thenReturn(whatsappPath);

        Predicate phonePredicate = mock(Predicate.class);
        Predicate whatsappPredicate = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);

        when(cb.like(phonePath, "%1234%")).thenReturn(phonePredicate);
        when(cb.like(whatsappPath, "%1234%")).thenReturn(whatsappPredicate);
        when(cb.or(phonePredicate, whatsappPredicate)).thenReturn(combined);

        Specification<Adult_MemberEntity> spec = MemberSpecifications.phoneContains("1234");
        Predicate actual = spec.toPredicate(root, query, cb);

        assertEquals(combined, actual);
    }

    @Test
    void filterByAddress_shouldSkipEmptyFields() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Adult_MemberEntity> root = mock(Root.class);

        Path<Object> addressPath = mock(Path.class);
        Path<Object> cityPath = mock(Path.class);
        Path<Object> zipcodePath = mock(Path.class);

        when(root.get("address")).thenReturn(addressPath);
        when(addressPath.get("city")).thenReturn(cityPath);
        when(addressPath.get("zipcode")).thenReturn(zipcodePath);

        Predicate cityPredicate = mock(Predicate.class);
        Predicate zipcodePredicate = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);

        when(cb.equal(cityPath, "Adama")).thenReturn(cityPredicate);
        when(cb.equal(zipcodePath, "1234")).thenReturn(zipcodePredicate);
        when(cb.and(cityPredicate, zipcodePredicate)).thenReturn(combined);

        Address address = Address.builder()
                .city("Adama")
                .country("")
                .province(null)
                .street("")
                .zipcode("1234")
                .build();

        Specification<Adult_MemberEntity> spec = MemberSpecifications.filterByAddress(address);
        Predicate actual = spec.toPredicate(root, query, cb);

        assertEquals(combined, actual);
    }

    @Test
    void filterByAddress_withNullAddress_returnsConjunction() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Adult_MemberEntity> root = mock(Root.class);
        Predicate expected = mock(Predicate.class);

        when(cb.and()).thenReturn(expected);

        Specification<Adult_MemberEntity> spec = MemberSpecifications.filterByAddress(null);
        Predicate actual = spec.toPredicate(root, query, cb);

        assertEquals(expected, actual);
    }
}
