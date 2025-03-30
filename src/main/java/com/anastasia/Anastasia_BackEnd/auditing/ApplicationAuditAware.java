package com.anastasia.Anastasia_BackEnd.auditing;

import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import com.anastasia.Anastasia_BackEnd.model.principal.UserPrincipal;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public class ApplicationAuditAware implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken){
            return Optional.empty();
        }

//        UserEntity user = (UserEntity) authentication.getPrincipal();
//        return Optional.ofNullable(user.getUuid());

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserPrincipal)) {
            return Optional.empty();
        }

        UserPrincipal userPrincipal = (UserPrincipal) principal;

        // Retrieve UserEntity from the database using UserPrincipal's identifier
        return Optional.ofNullable(userPrincipal.getUserUuid());
    }
}
