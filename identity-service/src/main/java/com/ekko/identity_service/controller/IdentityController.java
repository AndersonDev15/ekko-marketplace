package com.ekko.identity_service.controller;

import com.ekko.identity_service.dto.request.*;
import com.ekko.identity_service.dto.response.*;
import com.ekko.identity_service.service.IdentityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/identity")
@RequiredArgsConstructor
public class IdentityController {

    private final IdentityService identityService;

    @PostMapping("/register/customer")
    public ResponseEntity<RegisterResponse> registerCustomer(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = identityService.registerCustomer(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/seller")
    public ResponseEntity<RegisterResponse> registerSeller(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = identityService.registerSeller(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RegisterResponse> registerAdmin(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = identityService.registerAdmin(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/credentials")
    public ResponseEntity<UpdateCredentialsResponse> updateCredentials(@Valid @RequestBody UpdateCredentialsRequest request) {
        UpdateCredentialsResponse response = identityService.updateCredentials(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/credentials/password")
    public ResponseEntity<PasswordChangedResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        PasswordChangedResponse response = identityService.changePassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        identityService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/resend-verification")
    public ResponseEntity<Void> resendVerificationEmail() {
        identityService.resendVerificationEmail();
        return ResponseEntity.ok().build();
    }
}