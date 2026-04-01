package com.anastasia.Anastasia_BackEnd.TestControllers;

import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@Profile({"test", "test-server", "api"})  // Only available in test/api profiles
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
