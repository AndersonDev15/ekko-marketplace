package com.ekko.notification_service.service;

import com.ekko.notification_service.exception.UserLookupException;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class UserLookupService {

    private final Keycloak keycloak;
    private final String realm;

    public UserLookupService(Keycloak keycloak, @Value("${keycloak.admin.realm}") String realm) {
        this.keycloak = keycloak;
        this.realm = realm;
    }

    public String resolveEmail(UUID keycloakId) {
        try {
            UserRepresentation user =
                    keycloak.realm(realm).users().get(keycloakId.toString()).toRepresentation();
            String email = user.getEmail();
            if (email == null || email.isBlank()) {
                log.error("Keycloak user {} has no email set", keycloakId);
                throw new UserLookupException("Email is null or empty for keycloakId=" + keycloakId);
            }
            return email;
        } catch (UserLookupException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to resolve email for keycloakId={}", keycloakId, e);
            throw new UserLookupException("Failed to resolve email for keycloakId=" + keycloakId, e);
        }
    }
}