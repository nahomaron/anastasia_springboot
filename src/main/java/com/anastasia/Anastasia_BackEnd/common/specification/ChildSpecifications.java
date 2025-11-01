package com.anastasia.Anastasia_BackEnd.common.specification;


import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.child.ChildEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ChildSpecifications {

    public static Specification<ChildEntity> hasMembershipNumber(Long membershipNumber){
        return (root, query, cb) -> cb.equal(root.get("membershipNumber"), membershipNumber);
    }

    public static Specification<ChildEntity> hasStatus(String status){
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<ChildEntity> isDeacon(boolean deacon){
        return (root, query, cb) -> cb.equal(root.get("deacon"), deacon);
    }

    public static Specification<ChildEntity> nameContains(String name){
        return (root, query, cb) -> {
            String likePattern = "%" + name.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), likePattern),
                    cb.like(cb.lower(root.get("fatherName")), likePattern),
                    cb.like(cb.lower(root.get("grandFatherName")), likePattern),
                    cb.like(root.get("firstNameT"), likePattern),
                    cb.like(root.get("fatherNameT"), likePattern),
                    cb.like(root.get("grandFatherNameT"), likePattern)
            );
        };
    }

    public static Specification<ChildEntity> motherNameContains(String motherName){
        return (root, query, cb) -> {
            String likePattern = "%" + motherName.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("motherName")), likePattern),
                    cb.like(root.get("motherFullNameT"), likePattern)
            );
        };
    }

    public static Specification<ChildEntity> hasGender(String gender){
        return (root, query, cb) -> cb.equal(root.get("gender"), gender);
    }

    public static Specification<ChildEntity> ageBetween(int minAge, int maxAge) {
        return (root, query, cb) -> {
            LocalDate today = LocalDate.now();
            LocalDate maxDate = today.minusYears(minAge); // Youngest person
            LocalDate minDate = today.minusYears(maxAge); // Oldest person

            return cb.between(root.get("birthday"), minDate, maxDate);
        };
    }

    public static Specification<ChildEntity> phoneContains(String phone){
        String likePattern = "%" + phone + "%";
        return (root, query, cb) -> cb.or(
                cb.like(root.get("phone"), likePattern),
                cb.like(root.get("whatsApp"), likePattern)
        );
    }

    public static Specification<ChildEntity> hasLevelOfEducation(String levelOfEducation){
        String likePattern = "%" + levelOfEducation.toLowerCase() + "%";
        return (root, query, cb) ->  cb.like(cb.lower(root.get("levelOfEducation")), likePattern);
    }

    public static Specification<ChildEntity> filterByAddress(Address address) {
        return (root, query, cb) -> {
            if (address == null) {
                return cb.and();
            }

            List<Predicate> predicates = new ArrayList<>();
            var addressPath = root.get("address");

            if (StringUtils.hasText(address.getCity())) {
                predicates.add(cb.equal(addressPath.get("city"), address.getCity()));
            }
            if (StringUtils.hasText(address.getCountry())) {
                predicates.add(cb.equal(addressPath.get("country"), address.getCountry()));
            }
            if (StringUtils.hasText(address.getZipcode())) {
                predicates.add(cb.equal(addressPath.get("zipcode"), address.getZipcode()));
            }
            if (StringUtils.hasText(address.getProvince())) {
                predicates.add(cb.equal(addressPath.get("province"), address.getProvince()));
            }
            if (StringUtils.hasText(address.getStreet())) {
                predicates.add(cb.equal(addressPath.get("street"), address.getStreet()));
            }

            if (predicates.isEmpty()) {
                return cb.and();
            }

            Predicate combined = predicates.get(0);
            for (int i = 1; i < predicates.size(); i++) {
                combined = cb.and(combined, predicates.get(i));
            }

            return combined;
        };
    }


}
