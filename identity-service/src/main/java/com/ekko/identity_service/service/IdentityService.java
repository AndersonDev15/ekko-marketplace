package com.ekko.identity_service.service;

import com.ekko.identity_service.dto.request.*;
import com.ekko.identity_service.dto.response.*;
import com.ekko.identity_service.entity.OutboxEvent;
import com.ekko.identity_service.exception.*;
import com.ekko.identity_service.keycloak.KeycloakService;
import com.ekko.identity_service.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentityService {

    private final KeycloakService keycloakService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public RegisterResponse registerCustomer(RegisterRequest request) {
        String keycloakId = keycloakService.createUser(
                request.email(), request.firstName(), request.lastName(), request.password(), "CUSTOMER"
        );
        publishUserRegisteredEvent(keycloakId, request.email(), request.firstName(), request.lastName(), "CUSTOMER");
        return new RegisterResponse(keycloakId, request.email(), request.firstName(), request.lastName(), "CUSTOMER", Instant.now());
    }

    @Transactional
    public RegisterResponse registerSeller(RegisterRequest request) {
        String keycloakId = keycloakService.createUser(
                request.email(), request.firstName(), request.lastName(), request.password(), "SELLER"
        );
        publishUserRegisteredEvent(keycloakId, request.email(), request.firstName(), request.lastName(), "SELLER");
        return new RegisterResponse(keycloakId, request.email(), request.firstName(), request.lastName(), "SELLER", Instant.now());
    }

    @Transactional
    public RegisterResponse registerAdmin(RegisterRequest request) {
        String keycloakId = keycloakService.createUser(
                request.email(), request.firstName(), request.lastName(), request.password(), "ADMIN"
        );
        publishUserRegisteredEvent(keycloakId, request.email(), request.firstName(), request.lastName(), "ADMIN");
        return new RegisterResponse(keycloakId, request.email(), request.firstName(), request.lastName(), "ADMIN", Instant.now());
    }

    private void publishUserRegisteredEvent(String keycloakId, String email, String firstName, String lastName, String role) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "keycloakId", keycloakId,
                    "email", email,
                    "firstName", firstName,
                    "lastName", lastName,
                    "role", role,
                    "registeredAt", Instant.now().toString()
            ));
            String routingKey = "user.registered." + role.toLowerCase();
            OutboxEvent event = OutboxEvent.create(UUID.fromString(keycloakId), "UserRegisteredEvent", payload, routingKey);
            outboxEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to create outbox event for user registration", e);
        }
    }

    @Transactional
    public UpdateCredentialsResponse updateCredentials(UpdateCredentialsRequest request) {
        String keycloakId = getCurrentUserKeycloakId();
        keycloakService.updateUser(keycloakId, request.email(), request.firstName(), request.lastName());
        
        var user = keycloakService.getUserById(keycloakId);
        return new UpdateCredentialsResponse(
                keycloakId,
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                Instant.now()
        );
    }

    @Transactional
    public PasswordChangedResponse changePassword(ChangePasswordRequest request) {
        String keycloakId = getCurrentUserKeycloakId();
        keycloakService.changePassword(keycloakId, request.currentPassword(), request.newPassword());
        publishPasswordChangedEvent(keycloakId);
        return new PasswordChangedResponse(keycloakId, Instant.now());
    }

    private void publishPasswordChangedEvent(String keycloakId) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "keycloakId", keycloakId,
                    "changedAt", Instant.now().toString()
            ));
            OutboxEvent event = OutboxEvent.create(UUID.fromString(keycloakId), "PasswordChangedEvent", payload, "user.password-changed");
            outboxEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to create outbox event for password change", e);
        }
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        keycloakService.sendResetPasswordEmail(request.email());
    }

    @Transactional
    public void resendVerificationEmail() {
        String keycloakId = getCurrentUserKeycloakId();
        keycloakService.resendVerificationEmail(keycloakId);
    }

    private String getCurrentUserKeycloakId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No authenticated user found");
        }
        return jwt.getSubject();
    }
}