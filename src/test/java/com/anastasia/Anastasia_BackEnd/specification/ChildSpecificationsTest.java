package com.anastasia.Anastasia_BackEnd.specification;

import com.anastasia.Anastasia_BackEnd.model.child.ChildEntity;
import com.anastasia.Anastasia_BackEnd.model.common.Address;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
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

}
