package com.anastasia.Anastasia_BackEnd.TestControllers;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Profile({"test", "test-server", "api"})
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestLookupController {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ChurchRepository churchRepository;
    private final TenantRepository tenantRepository;

    @GetMapping("/roles")
    public List<RoleSummary> getRoles() {
        return roleRepository.findAll().stream()
                .map(role -> new RoleSummary(role.getId(), role.getRoleName()))
                .toList();
    }

    @GetMapping("/users/id")
    public ResponseEntity<UserIdentifier> findUserIdByEmail(@RequestParam String email) {
        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(new UserIdentifier(user.getUuid())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/users/link-tenant")
    public ResponseEntity<String> linkUserToTenant(@RequestParam String email) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tenant context missing");
        }

        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

        user.assignAffiliatedTenant(tenant);
        userRepository.save(user);
        return ResponseEntity.ok("User linked to tenant");
    }

    @GetMapping("/church/current")
    public ResponseEntity<ChurchIdentifier> getCurrentTenantChurch() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return churchRepository.findByTenantId(tenantId)
                .map(church -> ResponseEntity.ok(new ChurchIdentifier(church.getChurchId())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record RoleSummary(Long id, String roleName) {}

    public record UserIdentifier(UUID id) {}

    public record ChurchIdentifier(Long id) {}
}
