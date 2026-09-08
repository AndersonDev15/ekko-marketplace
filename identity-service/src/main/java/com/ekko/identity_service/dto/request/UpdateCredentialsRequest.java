package com.ekko.identity_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateCredentialsRequest(
        @Email String email,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName
) {
}