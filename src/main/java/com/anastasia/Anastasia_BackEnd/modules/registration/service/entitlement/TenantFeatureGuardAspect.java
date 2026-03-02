package com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class TenantFeatureGuardAspect {

    private final TenantEntitlementAccessService entitlementAccessService;

    @Around("@within(com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.RequiresTenantFeature) || " +
            "@annotation(com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.RequiresTenantFeature)")
    public Object guard(ProceedingJoinPoint joinPoint) throws Throwable {
        RequiresTenantFeature feature = findAnnotation(joinPoint);
        if (feature != null) {
            entitlementAccessService.requireFeature(feature.value());
        }
        return joinPoint.proceed();
    }

    private RequiresTenantFeature findAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequiresTenantFeature methodAnnotation = AnnotationUtils.findAnnotation(signature.getMethod(), RequiresTenantFeature.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), RequiresTenantFeature.class);
    }
}
