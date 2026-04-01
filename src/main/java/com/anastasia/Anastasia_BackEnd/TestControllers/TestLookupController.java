package com.anastasia.Anastasia_BackEnd.TestControllers;

import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Profile({"test", "test-server"})
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestLookupController {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

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

    public record RoleSummary(Long id, String roleName) {}

    public record UserIdentifier(UUID id) {}
}
