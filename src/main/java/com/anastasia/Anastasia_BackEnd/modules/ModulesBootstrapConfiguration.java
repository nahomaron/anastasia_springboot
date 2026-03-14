package com.anastasia.Anastasia_BackEnd.modules;

import com.anastasia.Anastasia_BackEnd.core.notification.config.NotificationModuleConfig;
import com.anastasia.Anastasia_BackEnd.modules.accounting.config.AccountingModuleConfig;
import com.anastasia.Anastasia_BackEnd.modules.appointments.config.AppointmentsModuleConfig;
import com.anastasia.Anastasia_BackEnd.modules.events.config.EventsModuleConfig;
import com.anastasia.Anastasia_BackEnd.modules.groups.config.GroupsModuleConfig;
import com.anastasia.Anastasia_BackEnd.modules.payments.config.PaymentsModuleConfig;
import com.anastasia.Anastasia_BackEnd.modules.registration.config.RegistrationModuleConfig;
import com.anastasia.Anastasia_BackEnd.modules.staff.config.StaffModuleConfig;
import com.anastasia.Anastasia_BackEnd.modules.users.config.UsersModuleConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Central toggle point for enabling individual modules inside the application.
 * By importing this configuration we make only the selected modules visible to Spring.
 */
@Configuration
@Import({
        AccountingModuleConfig.class,
        AppointmentsModuleConfig.class,
        EventsModuleConfig.class,
        GroupsModuleConfig.class,
        PaymentsModuleConfig.class,
        RegistrationModuleConfig.class,
        StaffModuleConfig.class,
        UsersModuleConfig.class,
        NotificationModuleConfig.class
})
public class ModulesBootstrapConfiguration {
}
