package com.ekko.notification_service.service;

import com.ekko.notification_service.exception.UserLookupException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLookupServiceTest {

    private static final String REALM = "ekko";

    @Mock
    private Keycloak keycloak;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    private UserRepresentation userRepresentation;

    private UserLookupService service;

    @BeforeEach
    void setUp() {
        service = new UserLookupService(keycloak, REALM);
        userRepresentation = new UserRepresentation();
        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
    }

    @Test
    void resolvesEmailWhenUserHasEmail() {
        UUID keycloakId = UUID.randomUUID();
        userRepresentation.setEmail("seller@ekko.test");
        when(usersResource.get(keycloakId.toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRepresentation);

        String email = service.resolveEmail(keycloakId);

        assertEquals("seller@ekko.test", email);
    }

    @Test
    void throwsWhenEmailIsNull() {
        UUID keycloakId = UUID.randomUUID();
        userRepresentation.setEmail(null);
        when(usersResource.get(keycloakId.toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRepresentation);

        assertThrows(UserLookupException.class, () -> service.resolveEmail(keycloakId));
    }

    @Test
    void throwsWhenEmailIsBlank() {
        UUID keycloakId = UUID.randomUUID();
        userRepresentation.setEmail("   ");
        when(usersResource.get(keycloakId.toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRepresentation);

        assertThrows(UserLookupException.class, () -> service.resolveEmail(keycloakId));
    }

    @Test
    void throwsWhenUserNotFound() {
        UUID keycloakId = UUID.randomUUID();
        when(usersResource.get(keycloakId.toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenThrow(new NotFoundException());

        assertThrows(UserLookupException.class, () -> service.resolveEmail(keycloakId));
    }

    @Test
    void throwsWhenKeycloakCallFails() {
        UUID keycloakId = UUID.randomUUID();
        when(usersResource.get(keycloakId.toString())).thenReturn(userResource);
        when(userResource.toRepresentation()).thenThrow(new RuntimeException("connection refused"));

        assertThrows(UserLookupException.class, () -> service.resolveEmail(keycloakId));
    }
}