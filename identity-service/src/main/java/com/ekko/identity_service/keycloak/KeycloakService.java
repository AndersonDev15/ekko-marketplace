package com.ekko.identity_service.keycloak;

import com.ekko.identity_service.exception.EmailAlreadyExistsException;
import com.ekko.identity_service.exception.InvalidPasswordException;
import com.ekko.identity_service.exception.KeycloakCommunicationException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Service
public class KeycloakService {

    private final Keycloak keycloak;
    private final String realm;

    public KeycloakService(Keycloak keycloak, KeycloakProperties properties) {
        this.keycloak = keycloak;
        this.realm = properties.getRealm();
    }

    private RealmResource realmResource() {
        return keycloak.realm(realm);
    }

    private UsersResource usersResource() {
        return realmResource().users();
    }

    public String createUser(String email, String firstName, String lastName, String password, String role) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(false);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        try (Response response = usersResource().create(user)) {
            if (response.getStatus() == 409 || (response.getStatus() >= 400 && response.getStatus() < 500)) {
                String error = response.readEntity(String.class);
                if (error != null && error.contains("exists")) {
                    throw new EmailAlreadyExistsException(email);
                }
            }
            if (response.getStatus() >= 400) {
                throw new KeycloakCommunicationException("Failed to create user in Keycloak: " + response.getStatus());
            }

            String location = response.getLocation().getPath();
            String userId = location.substring(location.lastIndexOf('/') + 1);

            assignRole(userId, role);
            sendVerifyEmail(userId);

            return userId;
        } catch (EmailAlreadyExistsException e) {
            throw e;
        } catch (Exception e) {
            throw new KeycloakCommunicationException("Failed to create user in Keycloak", e);
        }
    }

    public void assignRole(String userId, String role) {
        try {
            RoleRepresentation realmRole = realmResource().roles().get(role).toRepresentation();
            usersResource().get(userId).roles().realmLevel().add(List.of(realmRole));
        } catch (NotFoundException e) {
            throw new KeycloakCommunicationException("Role not found: " + role, e);
        } catch (Exception e) {
            throw new KeycloakCommunicationException("Failed to assign role: " + role, e);
        }
    }

    public void sendVerifyEmail(String userId) {
        try {
            usersResource().get(userId).executeActionsEmail(List.of("VERIFY_EMAIL"));
        } catch (Exception e) {
            throw new KeycloakCommunicationException("Failed to send verification email", e);
        }
    }

    public UserRepresentation getUserById(String userId) {
        try {
            return usersResource().get(userId).toRepresentation();
        } catch (NotFoundException e) {
            return null;
        } catch (Exception e) {
            throw new KeycloakCommunicationException("Failed to get user", e);
        }
    }

    public UserRepresentation getUserByEmail(String email) {
        try {
            List<UserRepresentation> users = usersResource().searchByEmail(email, true);
            return users.isEmpty() ? null : users.get(0);
        } catch (Exception e) {
            throw new KeycloakCommunicationException("Failed to search user by email", e);
        }
    }

    public void updateUser(String userId, String email, String firstName, String lastName) {
        try {
            UserRepresentation user = usersResource().get(userId).toRepresentation();
            if (email != null) {
                user.setEmail(email);
                user.setUsername(email);
            }
            if (firstName != null) {
                user.setFirstName(firstName);
            }
            if (lastName != null) {
                user.setLastName(lastName);
            }
            usersResource().get(userId).update(user);
        } catch (Exception e) {
            throw new KeycloakCommunicationException("Failed to update user", e);
        }
    }

    public void changePassword(String userId, String newPassword) {
        try {
            UserResource userResource = usersResource().get(userId);

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false);

            userResource.resetPassword(credential);
        } catch (jakarta.ws.rs.ProcessingException e) {
            throw new KeycloakCommunicationException("Failed to change password: Keycloak unavailable", e);
        } catch (jakarta.ws.rs.WebApplicationException e) {
            String message = e.getMessage();
            if (message != null && message.toLowerCase().contains("invalid")) {
                throw new InvalidPasswordException(message);
            }
            throw new KeycloakCommunicationException("Failed to change password", e);
        } catch (Exception e) {
            throw new KeycloakCommunicationException("Failed to change password", e);
        }
    }

    public void sendResetPasswordEmail(String email) {
        try {
            List<UserRepresentation> users = usersResource().searchByEmail(email, true);
            if (!users.isEmpty()) {
                usersResource().get(users.get(0).getId()).executeActionsEmail(List.of("UPDATE_PASSWORD"));
            }
        } catch (Exception e) {
            throw new KeycloakCommunicationException("Failed to send reset password email", e);
        }
    }

    public void resendVerificationEmail(String userId) {
        try {
            usersResource().get(userId).executeActionsEmail(List.of("VERIFY_EMAIL"));
        } catch (Exception e) {
            throw new KeycloakCommunicationException("Failed to resend verification email", e);
        }
    }

    public List<String> getUserRoles(String userId) {
        try {
            return usersResource().get(userId).roles().realmLevel().listAll().stream()
                    .map(RoleRepresentation::getName)
                    .toList();
        } catch (Exception e) {
            throw new KeycloakCommunicationException("Failed to get user roles", e);
        }
    }
}