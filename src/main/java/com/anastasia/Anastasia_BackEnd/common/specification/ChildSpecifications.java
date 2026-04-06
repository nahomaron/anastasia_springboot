package com.anastasia.Anastasia_BackEnd.common.specification;


import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChildSpecifications {

    public static Specification<Child_MemberEntity> hasMembershipNumber(Long membershipNumber){
        return (root, query, cb) -> cb.equal(root.get("membershipNumber"), membershipNumber);
    }

    public static Specification<Child_MemberEntity> hasStatus(String status){
        return (root, query, cb) -> cb.equal(root.get("statusValue"), status);
    }

    public static Specification<Child_MemberEntity> isDeacon(boolean deacon){
        return (root, query, cb) -> cb.equal(root.get("deacon"), deacon);
    }

    public static Specification<Child_MemberEntity> nameContains(String name){
        return (root, query, cb) -> {
            String likePattern = "%" + name.toLowerCase(Locale.ROOT) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), likePattern),
                    cb.like(cb.lower(root.get("fatherName")), likePattern),
                    cb.like(cb.lower(root.get("grandFatherName")), likePattern),
                    cb.like(root.get("firstNameLocal"), likePattern),
                    cb.like(root.get("fatherNameLocal"), likePattern),
                    cb.like(root.get("grandFatherNameLocal"), likePattern)
            );
        };
    }

    public static Specification<Child_MemberEntity> motherNameContains(String motherName){
        return (root, query, cb) -> {
            String likePattern = "%" + motherName.toLowerCase(Locale.ROOT) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("motherName")), likePattern),
                    cb.like(root.get("motherFullNameLocal"), likePattern)
            );
        };
    }

    public static Specification<Child_MemberEntity> hasGender(String gender){
        return (root, query, cb) -> cb.equal(root.get("gender"), gender);
    }

    public static Specification<Child_MemberEntity> ageBetween(int minAge, int maxAge) {
        return (root, query, cb) -> {
            LocalDate today = LocalDate.now();
            LocalDate maxDate = today.minusYears(minAge); // Youngest person
            LocalDate minDate = today.minusYears(maxAge); // Oldest person

            return cb.between(root.get("birthday"), minDate, maxDate);
        };
    }

    public static Specification<Child_MemberEntity> phoneContains(String phone){
        String likePattern = "%" + phone + "%";
        return (root, query, cb) -> cb.or(
                cb.like(root.get("phone"), likePattern),
                cb.like(root.get("whatsApp"), likePattern)
        );
    }

    public static Specification<Child_MemberEntity> hasLevelOfEducation(String levelOfEducation){
        String likePattern = "%" + levelOfEducation.toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) ->  cb.like(cb.lower(root.get("levelOfEducation")), likePattern);
    }

    public static Specification<Child_MemberEntity> filterByAddress(Address address) {
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
            if (StringUtils.hasText(address.getPostalCode())) {
                predicates.add(cb.equal(addressPath.get("postalCode"), address.getPostalCode()));
            }
            if (StringUtils.hasText(address.getStateProvince())) {
                predicates.add(cb.equal(addressPath.get("stateProvince"), address.getStateProvince()));
            }
            if (StringUtils.hasText(address.getAddressLine1())) {
                predicates.add(cb.equal(addressPath.get("addressLine1"), address.getAddressLine1()));
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
