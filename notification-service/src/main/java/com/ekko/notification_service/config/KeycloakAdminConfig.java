package com.ekko.notification_service.config;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Builds the Keycloak admin client used to resolve seller emails by keycloakId
 * (see {@link com.ekko.notification_service.service.UserLookupService}).
 *
 * <p>Manual Keycloak setup required (this code only calls the Admin REST API):
 * <ol>
 *   <li>In the {@code ekko} realm, create a client with client-id
 *       {@code notification-service-admin} and Access Type {@code confidential}
 *       (confidential client → Client Credentials grant).</li>
 *   <li>Set a client secret and expose it to the service via the environment
 *       variable {@code KEYCLOAK_ADMIN_CLIENT_SECRET}.</li>
 *   <li>In the {@code Service account roles} tab of that client, assign the
 *       {@code view-users} realm role (or {@code manage-users} if user management is needed later)
 *       so the Admin API allows reading user representations.</li>
 * </ol>
 */
@Configuration
public class KeycloakAdminConfig {

    private final String serverUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakAdminConfig(
            @Value("${keycloak.admin.server-url}") String serverUrl,
            @Value("${keycloak.admin.realm}") String realm,
            @Value("${keycloak.admin.client-id}") String clientId,
            @Value("${keycloak.admin.client-secret}") String clientSecret) {
        this.serverUrl = serverUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .resteasyClient(resteasyClient())
                .build();
    }

    private ResteasyClient resteasyClient() {
        return new ResteasyClientBuilderImpl()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .connectionTTL(5, TimeUnit.SECONDS)
                .build();
    }
}