package com.anastasia.Anastasia_BackEnd.UnitTests.config;

import com.anastasia.Anastasia_BackEnd.common.config.EmailConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;

class EmailConfigTest {

    @Test
    void javaMailSender_shouldConfigureHostPortAndCredentials() {
        EmailConfig config = new EmailConfig();

        JavaMailSenderImpl sender = (JavaMailSenderImpl) config.javaMailSender();

        assertThat(sender.getHost()).isEqualTo("smtp.mailtrap.io");
        assertThat(sender.getPort()).isEqualTo(2525);
        assertThat(sender.getUsername()).isEqualTo("319d5bf13c8d5c");
        assertThat(sender.getPassword()).isEqualTo("4675f8e0b6d44e");
        assertThat(sender.getJavaMailProperties()).containsEntry("mail.smtp.auth", "true");
        assertThat(sender.getJavaMailProperties()).containsEntry("mail.smtp.starttls.enable", "true");
    }
}
