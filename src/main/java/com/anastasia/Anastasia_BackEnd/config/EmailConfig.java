package com.anastasia.Anastasia_BackEnd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class EmailConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        // Set your SMTP server configurations
        mailSender.setHost("smtp.mailtrap.io");
        mailSender.setPort(2525);
        mailSender.setUsername(System.getenv("MAIL_USERNAME"));
        mailSender.setPassword(System.getenv("MAIL_PASSWORD"));

        // Optional configuration
        mailSender.setProtocol("smtp");
        mailSender.getJavaMailProperties().put("mail.smtp.auth", "true");
        mailSender.getJavaMailProperties().put("mail.smtp.starttls.enable", "true");

        return mailSender;
    }
}
