package com.anastasia.Anastasia_BackEnd.modules.publiccontact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class PublicContactRequest {

    @NotBlank
    @Size(max = 80)
    private String topic;

    @NotBlank
    @Size(min = 40, max = 2000)
    private String requestDescription;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(min = 2, max = 120)
    private String fullName;

    @NotBlank
    @Size(min = 2, max = 160)
    private String churchName;

    @Pattern(regexp = "^$|^\\+?[0-9()\\-\\s]{7,20}$")
    private String phone;

    private boolean textPermission;

    @NotBlank
    private String turnstileToken;

    private MultipartFile document;
}
