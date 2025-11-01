package com.anastasia.Anastasia_BackEnd.Api.tests.tenant;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.factories.TenantDataFactory;
import com.anastasia.Anastasia_BackEnd.Api.services.TenantService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.dto.PhoneVerificationRequest;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.dto.ResendOtpRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Tenant Subscription")
@Feature("Negative coverage")
@Severity(SeverityLevel.NORMAL)
class TenantNegativeTests extends BaseApiTest {

    private final TenantService tenantService = new TenantService();

    @Test
    @Story("Passwords must match for subscription")
    void subscriptionWithMismatchedPasswordsShouldFail() {
        TenantDTO tenant = TenantDataFactory.newValidTenant();
        tenant.setConfirmPassword("WrongPassword1!");

        Response response = tenantService.subscribeTenant(tenant);
        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
    }

    @Test
    @Story("Phone verification rejects wrong OTP")
    void verifyPhoneWithWrongOtpShouldFail() {
        PhoneVerificationRequest request = PhoneVerificationRequest.builder()
                .phone("+12025550000")
                .otp("0000")
                .build();

        Response response = tenantService.verifyPhone(request);
        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
    }

    @Test
    @Story("Resend OTP requires phone number")
    void resendOtpWithoutPhoneShouldFail() {
        ResendOtpRequest request = new ResendOtpRequest();
        Response response = tenantService.resendOtp(request);
        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
    }
}
