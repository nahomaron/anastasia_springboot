package com.anastasia.Anastasia_BackEnd.controller.test;

import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.repository.registration.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Profile("test")
@RestController
@RequestMapping("/test-utils/cleanup")
public class TestCleanupController {

    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private MemberRepository memberRepository;

    @DeleteMapping
    public String deleteUserByEmail(@RequestParam String email) {
        userRepository.findByEmail(email).ifPresent(userRepository::delete);
        return "Deleted user: " + email;
    }

    @DeleteMapping("/tenant")
    public String deleteTenantByEmail(@RequestParam String email) {
        tenantRepository.findByPhoneNumber(email).ifPresent(tenantRepository::delete);
        return "Deleted tenant: " + email;
    }

    @DeleteMapping("/member")
    public String deleteMemberById(@RequestParam Long id) {
        memberRepository.findById(id).ifPresent(memberRepository::delete);
        return "Deleted member: " + id;
    }

    @PostMapping("/reset-all")
    public String resetAll() {
        userRepository.deleteAll();
        tenantRepository.deleteAll();
        return "All test data cleared.";
    }
}
