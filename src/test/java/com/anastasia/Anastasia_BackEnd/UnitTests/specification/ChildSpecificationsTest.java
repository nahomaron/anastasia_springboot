package com.anastasia.Anastasia_BackEnd.UnitTests.specification;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.child.ChildEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.common.specification.ChildSpecifications;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ChildSpecificationsTest {

    @Test
    void testHasMembershipNumber() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<ChildEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Path<Long> path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        Mockito.<Path<Long>>when(root.get("membershipNumber")).thenReturn(path);
        when(cb.equal(path, 123L)).thenReturn(predicate);

        Specification<ChildEntity> spec = ChildSpecifications.hasMembershipNumber(123L);
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(predicate, result);
    }

    @Test
    void testIsDeacon() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<ChildEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Path<Boolean> path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        Mockito.<Path<Boolean>>when(root.get("deacon")).thenReturn(path);
        when(cb.equal(path, true)).thenReturn(predicate);

        Specification<ChildEntity> spec = ChildSpecifications.isDeacon(true);
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(predicate, result);
    }

    @Test
    void testHasStatus() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<ChildEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Path<String> path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        Mockito.<Path<String>>when(root.get("status")).thenReturn(path);
        when(cb.equal(path, "active")).thenReturn(predicate);

        Specification<ChildEntity> spec = ChildSpecifications.hasStatus("active");
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(predicate, result);
    }

    @Test
    void testHasGender() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<ChildEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Path<String> path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        Mockito.<Path<String>>when(root.get("gender")).thenReturn(path);
        when(cb.equal(path, "female")).thenReturn(predicate);

        Specification<ChildEntity> spec = ChildSpecifications.hasGender("female");
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(predicate, result);
    }

    @Test
    void testAgeBetween() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<ChildEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Path<LocalDate> path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);

        Mockito.<Path<LocalDate>>when(root.get("birthday")).thenReturn(path);
        when(cb.between(eq(path), any(LocalDate.class), any(LocalDate.class))).thenReturn(predicate);

        Specification<ChildEntity> spec = ChildSpecifications.ageBetween(10, 5);
        Predicate result = spec.toPredicate(root, query, cb);

        assertEquals(predicate, result);
    }

    @Test
    void testNameContains_buildsLowercaseSearchAcrossFields() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<ChildEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Predicate expected = mock(Predicate.class);

        Path<String> firstName = mock(Path.class);
        Path<String> fatherName = mock(Path.class);
        Path<String> grandFatherName = mock(Path.class);
        Path<String> firstNameT = mock(Path.class);
        Path<String> fatherNameT = mock(Path.class);
        Path<String> grandFatherNameT = mock(Path.class);

        Mockito.<Path<String>>when(root.get("firstName")).thenReturn(firstName);
        Mockito.<Path<String>>when(root.get("fatherName")).thenReturn(fatherName);
        Mockito.<Path<String>>when(root.get("grandFatherName")).thenReturn(grandFatherName);
        Mockito.<Path<String>>when(root.get("firstNameT")).thenReturn(firstNameT);
        Mockito.<Path<String>>when(root.get("fatherNameT")).thenReturn(fatherNameT);
        Mockito.<Path<String>>when(root.get("grandFatherNameT")).thenReturn(grandFatherNameT);

        Expression<String> expr1 = mock(Expression.class);
        Expression<String> expr2 = mock(Expression.class);
        Expression<String> expr3 = mock(Expression.class);

        when(cb.lower(firstName)).thenReturn(expr1);
        when(cb.lower(fatherName)).thenReturn(expr2);
        when(cb.lower(grandFatherName)).thenReturn(expr3);

        when(cb.like(expr1, "%john%")).thenReturn(mock(Predicate.class));
        when(cb.like(expr2, "%john%")).thenReturn(mock(Predicate.class));
        when(cb.like(expr3, "%john%")).thenReturn(mock(Predicate.class));
        when(cb.like(firstNameT, "%john%")).thenReturn(mock(Predicate.class));
        when(cb.like(fatherNameT, "%john%")).thenReturn(mock(Predicate.class));
        when(cb.like(grandFatherNameT, "%john%")).thenReturn(mock(Predicate.class));

        when(cb.or(any(), any(), any(), any(), any(), any())).thenReturn(expected);

        Specification<ChildEntity> specification = ChildSpecifications.nameContains("JoHn");
        Predicate actual = specification.toPredicate(root, query, cb);

        assertEquals(expected, actual);
    }

    @Test
    void filterByAddress_whenAddressNull_shouldReturnConjunction() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<ChildEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Predicate expected = mock(Predicate.class);

        when(cb.and()).thenReturn(expected);

        Specification<ChildEntity> spec = ChildSpecifications.filterByAddress(null);
        Predicate actual = spec.toPredicate(root, query, cb);

        assertEquals(expected, actual);
    }

    @Test
    void filterByAddress_withPartialFields_shouldAddOnlyNonBlankPredicates() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<ChildEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Predicate expected = mock(Predicate.class);

        Path<Object> addressPath = mock(Path.class);
        Path<Object> cityPath = mock(Path.class);
        Path<Object> countryPath = mock(Path.class);

        when(root.get("address")).thenReturn(addressPath);
        when(addressPath.get("city")).thenReturn(cityPath);
        when(addressPath.get("country")).thenReturn(countryPath);

        Predicate cityPredicate = mock(Predicate.class);
        Predicate countryPredicate = mock(Predicate.class);

        when(cb.equal(cityPath, "Addis")).thenReturn(cityPredicate);
        when(cb.equal(countryPath, "Ethiopia")).thenReturn(countryPredicate);
        when(cb.and(cityPredicate, countryPredicate)).thenReturn(expected);

        Address address = Address.builder()
                .city("Addis")
                .country("Ethiopia")
                .province("")
                .street("")
                .zipcode("")
                .build();

        Specification<ChildEntity> spec = ChildSpecifications.filterByAddress(address);
        Predicate actual = spec.toPredicate(root, query, cb);

        assertEquals(expected, actual);
    }
}
