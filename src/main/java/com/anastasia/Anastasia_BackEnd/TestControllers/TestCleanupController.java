package com.anastasia.Anastasia_BackEnd.TestControllers;

import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantAdminAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionEventRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile({"test", "api-tests"})  // Only available in test and API test profiles
@RestController
@RequestMapping("/api/v1/test-utils/cleanup")
@RequiredArgsConstructor
public class TestCleanupController {

    private final TokenRepository tokenRepository;
    private final ChildRepository childRepository;
    private final MemberRepository memberRepository;
    private final ChurchRepository churchRepository;
    private final TenantAdminAssignmentRepository tenantAdminAssignmentRepository;
    private final TenantSubscriptionEventRepository tenantSubscriptionEventRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

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
    @Transactional
    public String resetAll() {
        tokenRepository.deleteAllInBatch();
        childRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
        churchRepository.deleteAllInBatch();
        tenantAdminAssignmentRepository.deleteAllInBatch();
        tenantSubscriptionEventRepository.deleteAllInBatch();
        tenantSubscriptionRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        tenantRepository.deleteAllInBatch();
        return "All test data cleared.";
    }
}
