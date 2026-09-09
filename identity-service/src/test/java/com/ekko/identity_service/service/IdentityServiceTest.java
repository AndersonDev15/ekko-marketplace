package com.ekko.identity_service.service;

import com.ekko.identity_service.dto.request.*;
import com.ekko.identity_service.dto.response.*;
import com.ekko.identity_service.entity.OutboxEvent;
import com.ekko.identity_service.exception.EmailAlreadyExistsException;
import com.ekko.identity_service.exception.InvalidPasswordException;
import com.ekko.identity_service.exception.StepUpAuthRequiredException;
import com.ekko.identity_service.keycloak.KeycloakService;
import com.ekko.identity_service.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private Jwt jwt;

    private IdentityService identityService;

    @BeforeEach
    void setUp() {
        identityService = new IdentityService(keycloakService, outboxEventRepository, new com.fasterxml.jackson.databind.ObjectMapper());
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void registerCustomer_shouldCreateUserAndPublishEvent() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "John", "Doe");
        String keycloakId = UUID.randomUUID().toString();

        when(keycloakService.createUser(eq("test@example.com"), eq("John"), eq("Doe"), eq("password123"), eq("CUSTOMER")))
                .thenReturn(keycloakId);

        RegisterResponse response = identityService.registerCustomer(request);

        assertThat(response.keycloakId()).isEqualTo(keycloakId);
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.role()).isEqualTo("CUSTOMER");

        verify(keycloakService).createUser("test@example.com", "John", "Doe", "password123", "CUSTOMER");
        
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo("UserRegisteredEvent");
        assertThat(event.getRoutingKey()).isEqualTo("user.registered.customer");
    }

    @Test
    void registerSeller_shouldCreateUserAndPublishEvent() {
        RegisterRequest request = new RegisterRequest("seller@example.com", "password123", "Jane", "Smith");
        String keycloakId = UUID.randomUUID().toString();

        when(keycloakService.createUser(eq("seller@example.com"), eq("Jane"), eq("Smith"), eq("password123"), eq("SELLER")))
                .thenReturn(keycloakId);

        RegisterResponse response = identityService.registerSeller(request);

        assertThat(response.role()).isEqualTo("SELLER");
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getRoutingKey()).isEqualTo("user.registered.seller");
    }

    @Test
    void registerAdmin_shouldCreateUserAndPublishEvent() {
        RegisterRequest request = new RegisterRequest("admin@example.com", "password123", "Admin", "User");
        String keycloakId = UUID.randomUUID().toString();

        when(keycloakService.createUser(eq("admin@example.com"), eq("Admin"), eq("User"), eq("password123"), eq("ADMIN")))
                .thenReturn(keycloakId);

        RegisterResponse response = identityService.registerAdmin(request);

        assertThat(response.role()).isEqualTo("ADMIN");
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getRoutingKey()).isEqualTo("user.registered.admin");
    }

    @Test
    void registerCustomer_whenEmailExists_shouldThrowException() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "John", "Doe");

        when(keycloakService.createUser(any(), any(), any(), any(), any()))
                .thenThrow(new EmailAlreadyExistsException("test@example.com"));

        assertThatThrownBy(() -> identityService.registerCustomer(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void updateCredentials_shouldUpdateUserAndReturnResponse() {
        String keycloakId = UUID.randomUUID().toString();
        UpdateCredentialsRequest request = new UpdateCredentialsRequest("new@example.com", "Jane", "Smith");
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(keycloakId);

        org.keycloak.representations.idm.UserRepresentation user = mock(org.keycloak.representations.idm.UserRepresentation.class);
        when(keycloakService.getUserById(keycloakId)).thenReturn(user);
        when(user.getEmail()).thenReturn("new@example.com");
        when(user.getFirstName()).thenReturn("Jane");
        when(user.getLastName()).thenReturn("Smith");

        UpdateCredentialsResponse response = identityService.updateCredentials(request);

        assertThat(response.keycloakId()).isEqualTo(keycloakId);
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.firstName()).isEqualTo("Jane");
        assertThat(response.lastName()).isEqualTo("Smith");

        verify(keycloakService).updateUser(keycloakId, "new@example.com", "Jane", "Smith");
    }

    @Test
    void changePassword_withRecentAuthTime_shouldChangePasswordAndPublishEvent() {
        String keycloakId = UUID.randomUUID().toString();
        ChangePasswordRequest request = new ChangePasswordRequest("newPassword123");
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(keycloakId);
        // auth_time within 5 minutes (recent)
        long authTime = Instant.now().minus(2, ChronoUnit.MINUTES).getEpochSecond();
        when(jwt.getClaim("auth_time")).thenReturn(authTime);

        PasswordChangedResponse response = identityService.changePassword(request);

        assertThat(response.keycloakId()).isEqualTo(keycloakId);
        verify(keycloakService).changePassword(keycloakId, "newPassword123");
        
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("PasswordChangedEvent");
        assertThat(captor.getValue().getRoutingKey()).isEqualTo("user.password-changed");
    }

    @Test
    void changePassword_withOldAuthTime_shouldThrowStepUpAuthRequired() {
        String keycloakId = UUID.randomUUID().toString();
        ChangePasswordRequest request = new ChangePasswordRequest("newPassword123");
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        // auth_time older than 5 minutes
        long authTime = Instant.now().minus(10, ChronoUnit.MINUTES).getEpochSecond();
        when(jwt.getClaim("auth_time")).thenReturn(authTime);

        assertThatThrownBy(() -> identityService.changePassword(request))
                .isInstanceOf(StepUpAuthRequiredException.class)
                .hasMessageContaining("Recent re-authentication required to change password");
        
        verify(keycloakService, never()).changePassword(anyString(), anyString());
    }

    @Test
    void changePassword_withoutAuthTimeClaim_shouldThrowStepUpAuthRequired() {
        String keycloakId = UUID.randomUUID().toString();
        ChangePasswordRequest request = new ChangePasswordRequest("newPassword123");
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        // No auth_time claim
        when(jwt.getClaim("auth_time")).thenReturn(null);

        assertThatThrownBy(() -> identityService.changePassword(request))
                .isInstanceOf(StepUpAuthRequiredException.class)
                .hasMessageContaining("Token does not contain auth_time claim");
        
        verify(keycloakService, never()).changePassword(anyString(), anyString());
    }

    @Test
    void changePassword_whenInvalidPassword_shouldThrowException() {
        String keycloakId = UUID.randomUUID().toString();
        ChangePasswordRequest request = new ChangePasswordRequest("weak");
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(keycloakId);
        long authTime = Instant.now().minus(2, ChronoUnit.MINUTES).getEpochSecond();
        when(jwt.getClaim("auth_time")).thenReturn(authTime);
        
        doThrow(new InvalidPasswordException("Password too weak"))
                .when(keycloakService).changePassword(anyString(), anyString());

        assertThatThrownBy(() -> identityService.changePassword(request))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessageContaining("Password does not meet policy requirements");
    }

    @Test
    void forgotPassword_shouldSendResetEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

        identityService.forgotPassword(request);

        verify(keycloakService).sendResetPasswordEmail("test@example.com");
    }

    @Test
    void resendVerificationEmail_shouldSendEmail() {
        String keycloakId = UUID.randomUUID().toString();
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(keycloakId);

        identityService.resendVerificationEmail();

        verify(keycloakService).resendVerificationEmail(keycloakId);
    }
}