package com.anastasia.Anastasia_BackEnd.specification;


import com.anastasia.Anastasia_BackEnd.model.common.Address;
import com.anastasia.Anastasia_BackEnd.model.child.ChildEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

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
            List<Predicate> predicates = new ArrayList<>();

            if (address != null) {
                if (address.getCity() != null && !address.getCity().isEmpty()) {
                    predicates.add(cb.equal(root.get("address").get("city"), address.getCity()));
                }
                if (address.getCountry() != null && !address.getCountry().isEmpty()) {
                    predicates.add(cb.equal(root.get("address").get("country"), address.getCountry()));
                }
                if (address.getZipcode() != null && !address.getZipcode().isEmpty()) {
                    predicates.add(cb.equal(root.get("address").get("zipcode"), address.getZipcode()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }


}
