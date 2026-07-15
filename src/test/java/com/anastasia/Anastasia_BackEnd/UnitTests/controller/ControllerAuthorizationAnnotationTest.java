package com.anastasia.Anastasia_BackEnd.UnitTests.controller;

import com.anastasia.Anastasia_BackEnd.modules.accounting.controller.AccountController;
import com.anastasia.Anastasia_BackEnd.modules.accounting.controller.FundController;
import com.anastasia.Anastasia_BackEnd.modules.accounting.controller.ImportExportController;
import com.anastasia.Anastasia_BackEnd.modules.accounting.controller.ReconciliationController;
import com.anastasia.Anastasia_BackEnd.modules.accounting.controller.ReportController;
import com.anastasia.Anastasia_BackEnd.modules.accounting.controller.TransactionController;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.CreateAccountRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.CreateFundRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.GenerateReportRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.RecordExpenseRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.RecordIncomeRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.TransferFundsRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.UpdateFundRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.AccountType;
import com.anastasia.Anastasia_BackEnd.modules.appointments.controller.AppointmentController;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentCreateRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentRescheduleRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentStatusUpdateRequest;
import com.anastasia.Anastasia_BackEnd.modules.groups.GroupController;
import com.anastasia.Anastasia_BackEnd.modules.groups.dto.GroupJoinRequestDecisionRequest;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.controller.PaymentController;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.controller.PaymentQueryController;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.controller.SubscriptionQueryController;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.CreateIntentRequest;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.CreateSubscriptionRequest;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.controller.PlatformAdminController;
import com.anastasia.Anastasia_BackEnd.modules.registration.controller.ChildController;
import com.anastasia.Anastasia_BackEnd.modules.registration.controller.MemberController;
import com.anastasia.Anastasia_BackEnd.modules.registration.controller.PlatformSubscriptionAdminController;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.staff.controller.StaffController;
import com.anastasia.Anastasia_BackEnd.modules.staff.dto.CreateStaffRequest;
import com.anastasia.Anastasia_BackEnd.modules.staff.dto.UpdateStaffRequest;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEmploymentStatus;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ControllerAuthorizationAnnotationTest {

    private static final String ACCOUNTING_MANAGE =
            "@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'MANAGE_ACCOUNTS')";
    private static final String ACCOUNTING_READ =
            "@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'VIEW_ACCOUNTS', 'MANAGE_ACCOUNTS')";
    private static final String FUNDS_MANAGE =
            "@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'MANAGE_FUNDS')";
    private static final String FUNDS_READ =
            "@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'VIEW_FUNDS', 'MANAGE_FUNDS')";
    private static final String TRANSACTIONS_MANAGE =
            "@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'RECORD_TRANSACTIONS')";
    private static final String TRANSACTIONS_READ =
            "@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'RECORD_TRANSACTIONS', 'VIEW_FINANCE_REPORT')";

    @Test
    void memberAndChildSelfRegistration_shouldRequireAuthenticationOnly() throws NoSuchMethodException {
        assertThat(preAuthorize(MemberController.class, "registerMember", Adult_MemberDTO.class))
                .isEqualTo("isAuthenticated()");
        assertThat(preAuthorize(ChildController.class, "registerChild", Child_MemberDTO.class))
                .isEqualTo("isAuthenticated()");
    }

    @Test
    void appointmentReadEndpoints_shouldAllowViewAppointments() throws NoSuchMethodException {
        assertThat(preAuthorize(AppointmentController.class, "listAppointments",
                java.time.Instant.class,
                java.time.Instant.class,
                com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatus.class,
                com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentType.class))
                .contains("VIEW_APPOINTMENTS")
                .contains("MANAGE_APPOINTMENT");

        assertThat(preAuthorize(AppointmentController.class, "getAppointment", UUID.class))
                .contains("VIEW_APPOINTMENTS")
                .contains("MANAGE_APPOINTMENT");
    }

    @Test
    void appointmentMutationEndpoints_shouldRemainManageOnlyExceptMemberSelfService() throws NoSuchMethodException {
        assertThat(preAuthorize(AppointmentController.class, "rescheduleAppointment", UUID.class, AppointmentRescheduleRequest.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_APPOINTMENT')");
        assertThat(preAuthorize(AppointmentController.class, "updateStatus", UUID.class, AppointmentStatusUpdateRequest.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_APPOINTMENT')");
        assertThat(preAuthorize(AppointmentController.class, "addAssignees", UUID.class, List.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_APPOINTMENT')");
        assertThat(preAuthorize(AppointmentController.class, "removeAssignee", UUID.class, UUID.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_APPOINTMENT')");
        assertThat(preAuthorize(AppointmentController.class, "addParticipants", UUID.class, List.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_APPOINTMENT')");
        assertThat(preAuthorize(AppointmentController.class, "removeParticipant", UUID.class, UUID.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_APPOINTMENT')");

        assertThat(preAuthorize(AppointmentController.class, "createAppointment", AppointmentCreateRequest.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_APPOINTMENT', 'BOOK_APPOINTMENT')");
        assertThat(preAuthorize(AppointmentController.class, "rescheduleMyAppointment", UUID.class, AppointmentRescheduleRequest.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'BOOK_APPOINTMENT')");
        assertThat(preAuthorize(AppointmentController.class, "updateMyStatus", UUID.class, AppointmentStatusUpdateRequest.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'CANCEL_APPOINTMENT')");
    }

    @Test
    void groupJoinRequestModeration_shouldStayBehindGroupSecuritySupport() throws NoSuchMethodException {
        String expected = "@groupSecuritySupport.canManageGroup(authentication, #groupId)";

        assertThat(preAuthorize(GroupController.class, "listJoinRequests", Long.class, Authentication.class))
                .isEqualTo(expected);
        assertThat(preAuthorize(GroupController.class, "approveJoinRequest",
                Long.class,
                Long.class,
                GroupJoinRequestDecisionRequest.class,
                Authentication.class))
                .isEqualTo(expected);
        assertThat(preAuthorize(GroupController.class, "rejectJoinRequest",
                Long.class,
                Long.class,
                GroupJoinRequestDecisionRequest.class,
                Authentication.class))
                .isEqualTo(expected);
    }

    @Test
    void accountingControllers_shouldUsePermissionSpecificReadWriteGuards() throws NoSuchMethodException {
        assertThat(preAuthorize(AccountController.class, "createInitialChartOfAccounts", UUID.class))
                .isEqualTo(ACCOUNTING_MANAGE);
        assertThat(preAuthorize(AccountController.class, "createAccount", CreateAccountRequest.class))
                .isEqualTo(ACCOUNTING_MANAGE);
        assertThat(preAuthorize(AccountController.class, "getAccounts", UUID.class, AccountType.class))
                .isEqualTo(ACCOUNTING_READ);
        assertThat(preAuthorize(AccountController.class, "getAccountById", Long.class, UUID.class))
                .isEqualTo(ACCOUNTING_READ);
        assertThat(preAuthorize(AccountController.class, "updateAccount", Long.class, UUID.class, CreateAccountRequest.class))
                .isEqualTo(ACCOUNTING_MANAGE);
        assertThat(preAuthorize(AccountController.class, "deleteAccount", Long.class, UUID.class))
                .isEqualTo(ACCOUNTING_MANAGE);

        assertThat(preAuthorize(FundController.class, "createFund", CreateFundRequest.class))
                .isEqualTo(FUNDS_MANAGE);
        assertThat(preAuthorize(FundController.class, "getFunds", UUID.class))
                .isEqualTo(FUNDS_READ);
        assertThat(preAuthorize(FundController.class, "getFundById", Long.class, UUID.class))
                .isEqualTo(FUNDS_READ);
        assertThat(preAuthorize(FundController.class, "updateFund", Long.class, UpdateFundRequest.class))
                .isEqualTo(FUNDS_MANAGE);
        assertThat(preAuthorize(FundController.class, "deleteFund", Long.class, UUID.class))
                .isEqualTo(FUNDS_MANAGE);

        assertThat(preAuthorize(TransactionController.class, "recordIncome", RecordIncomeRequest.class))
                .isEqualTo(TRANSACTIONS_MANAGE);
        assertThat(preAuthorize(TransactionController.class, "recordExpense", RecordExpenseRequest.class))
                .isEqualTo(TRANSACTIONS_MANAGE);
        assertThat(preAuthorize(TransactionController.class, "transferFunds", TransferFundsRequest.class))
                .isEqualTo(TRANSACTIONS_MANAGE);
        assertThat(preAuthorize(TransactionController.class, "getTransactionById", Long.class, UUID.class))
                .isEqualTo(TRANSACTIONS_READ);
        assertThat(preAuthorize(TransactionController.class, "getTransactions",
                UUID.class, LocalDate.class, LocalDate.class, Long.class))
                .isEqualTo(TRANSACTIONS_READ);

        assertThat(preAuthorize(ReportController.class, "generateReport", GenerateReportRequest.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'GENERATE_FINANCE_REPORT', 'VIEW_FINANCE_REPORT')");
        assertThat(preAuthorize(ImportExportController.class, "exportToQuickBooks",
                UUID.class, LocalDate.class, LocalDate.class, HttpServletResponse.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'EXPORT_FINANCIAL_DATA')");
        assertThat(preAuthorize(ImportExportController.class, "importFromQuickBooks", UUID.class, MultipartFile.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'IMPORT_FINANCIAL_DATA')");
        assertThat(classPreAuthorize(ReconciliationController.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'RECONCILE_ACCOUNTS')");
    }

    @Test
    void paymentControllers_shouldRequireFinanceOrSubscriptionPermissionsAndTenantContextInService() throws NoSuchMethodException {
        assertThat(preAuthorize(PaymentController.class, "create", String.class, CreateIntentRequest.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'MANAGE_DONATIONS', 'RECORD_TRANSACTIONS')");
        assertThat(preAuthorize(PaymentController.class, "createSubscription", String.class, CreateSubscriptionRequest.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'OWN_SUBSCRIPTION', 'MANAGE_TENANT_BILLING', 'MANAGE_FINANCE')");
        assertThat(preAuthorize(PaymentQueryController.class, "findAll", Pageable.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'VIEW_FINANCE_REPORT', 'VIEW_DONATION_REPORTS')");
        assertThat(preAuthorize(PaymentQueryController.class, "findById", UUID.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'VIEW_FINANCE_REPORT', 'VIEW_DONATION_REPORTS')");
        assertThat(preAuthorize(PaymentQueryController.class, "totalPerFund"))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'VIEW_FINANCE_REPORT', 'VIEW_DONATION_REPORTS')");
        assertThat(preAuthorize(SubscriptionQueryController.class, "findAll", Pageable.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'OWN_SUBSCRIPTION', 'MANAGE_TENANT_BILLING', 'MANAGE_FINANCE', 'VIEW_FINANCE_REPORT')");
        assertThat(preAuthorize(SubscriptionQueryController.class, "findById", UUID.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'OWN_SUBSCRIPTION', 'MANAGE_TENANT_BILLING', 'MANAGE_FINANCE', 'VIEW_FINANCE_REPORT')");
    }

    @Test
    void staffController_shouldSeparateReadManageAndCredentialResetPermissions() throws NoSuchMethodException {
        assertThat(classPreAuthorize(StaffController.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_STAFF', 'VIEW_STAFF', 'RESET_STAFF_CREDENTIALS')");
        assertThat(preAuthorize(StaffController.class, "create", CreateStaffRequest.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_STAFF')");
        assertThat(preAuthorize(StaffController.class, "list", String.class, StaffEmploymentStatus.class, Pageable.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_STAFF', 'VIEW_STAFF')");
        assertThat(preAuthorize(StaffController.class, "getById", Long.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_STAFF', 'VIEW_STAFF')");
        assertThat(preAuthorize(StaffController.class, "update", Long.class, UpdateStaffRequest.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_STAFF')");
        assertThat(preAuthorize(StaffController.class, "deactivate", Long.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_STAFF')");
        assertThat(preAuthorize(StaffController.class, "resetCredentials", Long.class))
                .isEqualTo("@permissionEvaluator.hasAny(authentication, 'MANAGE_STAFF', 'RESET_STAFF_CREDENTIALS')");
    }

    @Test
    void platformControllers_shouldRemainPlatformAdminOnly() {
        assertThat(classPreAuthorize(PlatformAdminController.class)).isEqualTo("hasRole('PLATFORM_ADMIN')");
        assertThat(classPreAuthorize(PlatformSubscriptionAdminController.class)).isEqualTo("hasRole('PLATFORM_ADMIN')");
    }

    private String preAuthorize(Class<?> controllerClass, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = controllerClass.getMethod(methodName, parameterTypes);
        return method.getAnnotation(PreAuthorize.class).value();
    }

    private String classPreAuthorize(Class<?> controllerClass) {
        return controllerClass.getAnnotation(PreAuthorize.class).value();
    }
}
