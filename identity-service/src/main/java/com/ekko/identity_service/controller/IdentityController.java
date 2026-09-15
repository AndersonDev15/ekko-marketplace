package com.ekko.identity_service.controller;

import com.ekko.identity_service.dto.request.*;
import com.ekko.identity_service.dto.response.*;
import com.ekko.identity_service.exception.ErrorResponse;
import com.ekko.identity_service.service.IdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/identity")
@RequiredArgsConstructor
@Tag(name = "Identity", description = "User identity management endpoints")
public class IdentityController {

    private final IdentityService identityService;

    @Operation(
            summary = "Register a new customer",
            description = "Creates a new customer account in Keycloak with CUSTOMER role. Public endpoint - no authentication required."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer registered successfully",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Keycloak unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register/customer")
    public ResponseEntity<RegisterResponse> registerCustomer(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = identityService.registerCustomer(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Register a new seller",
            description = "Creates a new seller account in Keycloak with SELLER role. Public endpoint - no authentication required."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Seller registered successfully",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Keycloak unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register/seller")
    public ResponseEntity<RegisterResponse> registerSeller(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = identityService.registerSeller(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Register a new admin",
            description = "Creates a new admin account in Keycloak with ADMIN role. Requires ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Admin registered successfully",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Keycloak unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register/admin")
    public ResponseEntity<RegisterResponse> registerAdmin(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = identityService.registerAdmin(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update user credentials",
            description = "Updates the authenticated user's email, first name, and/or last name. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Credentials updated successfully",
                    content = @Content(schema = @Schema(implementation = UpdateCredentialsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Keycloak unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/credentials")
    public ResponseEntity<UpdateCredentialsResponse> updateCredentials(@Valid @RequestBody UpdateCredentialsRequest request) {
        UpdateCredentialsResponse response = identityService.updateCredentials(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Change user password",
            description = "Changes the authenticated user's password. Requires authentication and recent re-authentication (auth_time claim within 5 minutes)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully",
                    content = @Content(schema = @Schema(implementation = PasswordChangedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - recent re-authentication required (auth_time claim missing or older than 5 minutes)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Keycloak unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/credentials/password")
    public ResponseEntity<PasswordChangedResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        PasswordChangedResponse response = identityService.changePassword(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Request password reset",
            description = "Sends a password reset email to the specified email address. Public endpoint - no authentication required."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset email sent (always returns 200 for security)"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Keycloak unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        identityService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Resend email verification",
            description = "Resends the email verification link to the authenticated user. Requires authentication."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification email sent"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Keycloak unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/email/resend-verification")
    public ResponseEntity<Void> resendVerificationEmail() {
        identityService.resendVerificationEmail();
        return ResponseEntity.ok().build();
    }
}