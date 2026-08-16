package com.ekko.seller_service.service;

import com.ekko.seller_service.dto.request.SellerUpdateRequest;
import com.ekko.seller_service.dto.response.SellerResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerMetrics;
import com.ekko.seller_service.enums.SellerStatus;
import com.ekko.seller_service.exception.SellerNotFoundException;
import com.ekko.seller_service.exception.SellerSuspendedException;
import com.ekko.seller_service.mapper.SellerMapper;
import com.ekko.seller_service.messaging.SellerEventPublisher;
import com.ekko.seller_service.messaging.dto.SellerCreatedEvent;
import com.ekko.seller_service.repository.SellerMetricsRepository;
import com.ekko.seller_service.repository.SellerRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static com.ekko.seller_service.support.SellerTestDataBuilder.aSeller;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerProfileServiceTest {

    private static final String KEYCLOAK_ID = "kc-test-0001";
    private static final String EMAIL = "seller@ekko.test";

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerMetricsRepository metricsRepository;

    @Mock
    private SellerMapper sellerMapper;

    @Mock
    private SellerEventPublisher sellerEventPublisher;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    @InjectMocks
    private SellerProfileService sellerProfileService;

    @Nested
    class GetOrCreateMyProfile {

        @Test
        void vendedorExistente_devuelvePerfilSinCrear() {
            Seller seller = aSeller().withKeycloakId(KEYCLOAK_ID).build();
            SellerResponse expected = sellerResponseFor(seller);
            when(sellerRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(seller));
            when(sellerMapper.toResponse(seller)).thenReturn(expected);

            SellerResponse result = sellerProfileService.getOrCreateMyProfile(KEYCLOAK_ID, EMAIL);

            assertEquals(expected, result);
            verify(sellerRepository, never()).saveAndFlush(any(Seller.class));
            verify(metricsRepository, never()).saveAndFlush(any(SellerMetrics.class));
            verify(sellerEventPublisher, never()).publishSellerCreated(any(SellerCreatedEvent.class));
        }

        @Test
        void vendedorInexistente_creaPerfilConEstadoPendingReview() {
            Seller saved = aSeller()
                    .withKeycloakId(KEYCLOAK_ID)
                    .withEmail(EMAIL)
                    .pendingReview()
                    .build();
            SellerResponse expected = sellerResponseFor(saved);
            when(sellerRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.empty());
            when(sellerRepository.saveAndFlush(any(Seller.class))).thenAnswer(inv -> inv.getArgument(0));
            when(metricsRepository.saveAndFlush(any(SellerMetrics.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sellerMapper.toResponse(any(Seller.class))).thenReturn(expected);
            when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);

            SellerResponse result = sellerProfileService.getOrCreateMyProfile(KEYCLOAK_ID, EMAIL);

            assertEquals(expected, result);
            ArgumentCaptor<Seller> sellerCaptor = ArgumentCaptor.forClass(Seller.class);
            verify(sellerRepository).saveAndFlush(sellerCaptor.capture());
            Seller created = sellerCaptor.getValue();
            assertEquals(KEYCLOAK_ID, created.getKeycloakId());
            assertEquals(EMAIL, created.getEmail());
            assertEquals("Mi tienda", created.getStoreName());
            assertEquals(SellerStatus.PENDING_REVIEW, created.getStatus());
            verify(metricsRepository).saveAndFlush(any(SellerMetrics.class));

            ArgumentCaptor<SellerCreatedEvent> eventCaptor = ArgumentCaptor.forClass(SellerCreatedEvent.class);
            verify(sellerEventPublisher).publishSellerCreated(eventCaptor.capture());
            SellerCreatedEvent event = eventCaptor.getValue();
            assertEquals(KEYCLOAK_ID, event.keycloakId());
            assertEquals(EMAIL, event.email());
            assertEquals("Mi tienda", event.storeName());
            assertEquals(SellerStatus.PENDING_REVIEW, event.status());
        }

        @Test
        void carrera_recuperaPerfilExistente() {
            Seller existing = aSeller().withKeycloakId(KEYCLOAK_ID).build();
            SellerResponse expected = sellerResponseFor(existing);
            DataIntegrityViolationException race = dataIntegrityViolation(
                    "uk_sellers_keycloak_id", "23505");
            when(sellerRepository.findByKeycloakId(KEYCLOAK_ID))
                    .thenReturn(Optional.empty(), Optional.of(existing));
            when(sellerRepository.saveAndFlush(any(Seller.class))).thenThrow(race);
            when(sellerMapper.toResponse(existing)).thenReturn(expected);
            when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);

            SellerResponse result = sellerProfileService.getOrCreateMyProfile(KEYCLOAK_ID, EMAIL);

            assertEquals(expected, result);
            verify(metricsRepository, never()).saveAndFlush(any(SellerMetrics.class));
            verify(sellerRepository).saveAndFlush(any(Seller.class));
            verify(sellerEventPublisher, never()).publishSellerCreated(any(SellerCreatedEvent.class));
        }

        @Test
        void carrera_conSqlStateDiferente_relanzaExcepcion() {
            DataIntegrityViolationException violation = dataIntegrityViolation(
                    "uk_sellers_keycloak_id", "40001");
            when(sellerRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.empty());
            when(sellerRepository.saveAndFlush(any(Seller.class))).thenThrow(violation);
            when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);

            assertThrows(DataIntegrityViolationException.class,
                    () -> sellerProfileService.getOrCreateMyProfile(KEYCLOAK_ID, EMAIL));

            verify(metricsRepository, never()).saveAndFlush(any(SellerMetrics.class));
            verify(sellerEventPublisher, never()).publishSellerCreated(any(SellerCreatedEvent.class));
        }

        @Test
        void carrera_conConstraintDiferente_relanzaExcepcion() {
            DataIntegrityViolationException violation = dataIntegrityViolation(
                    "uk_sellers_email", "23505");
            when(sellerRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.empty());
            when(sellerRepository.saveAndFlush(any(Seller.class))).thenThrow(violation);
            when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);

            assertThrows(DataIntegrityViolationException.class,
                    () -> sellerProfileService.getOrCreateMyProfile(KEYCLOAK_ID, EMAIL));

            verify(metricsRepository, never()).saveAndFlush(any(SellerMetrics.class));
            verify(sellerEventPublisher, never()).publishSellerCreated(any(SellerCreatedEvent.class));
        }

        @Test
        void carrera_conCausaNoSql_relanzaExcepcion() {
            DataIntegrityViolationException violation =
                    new DataIntegrityViolationException("stmt", new RuntimeException("boom"));
            when(sellerRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.empty());
            when(sellerRepository.saveAndFlush(any(Seller.class))).thenThrow(violation);
            when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);

            assertThrows(DataIntegrityViolationException.class,
                    () -> sellerProfileService.getOrCreateMyProfile(KEYCLOAK_ID, EMAIL));

            verify(metricsRepository, never()).saveAndFlush(any(SellerMetrics.class));
            verify(sellerEventPublisher, never()).publishSellerCreated(any(SellerCreatedEvent.class));
        }
    }

    @Nested
    class UpdateMyProfile {

        @Test
        void vendedorActivo_actualizaDatos() {
            Seller seller = aSeller().withKeycloakId(KEYCLOAK_ID).active().build();
            SellerUpdateRequest request = new SellerUpdateRequest("Nueva tienda", "3110000000", "Descripción");
            SellerResponse expected = sellerResponseFor(seller);
            when(sellerRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(seller));
            when(sellerRepository.save(seller)).thenReturn(seller);
            when(sellerMapper.toResponse(seller)).thenReturn(expected);

            SellerResponse result = sellerProfileService.updateMyProfile(KEYCLOAK_ID, request);

            assertEquals(expected, result);
            assertEquals("Nueva tienda", seller.getStoreName());
            assertEquals("3110000000", seller.getPhone());
            assertEquals("Descripción", seller.getDescription());
            verify(sellerRepository).save(seller);
            verify(sellerEventPublisher, never()).publishSellerCreated(any(SellerCreatedEvent.class));
        }

        @Test
        void vendedorInexistente_lanzaSellerNotFound() {
            SellerUpdateRequest request = new SellerUpdateRequest("Tienda", null, null);
            when(sellerRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.empty());

            assertThrows(SellerNotFoundException.class,
                    () -> sellerProfileService.updateMyProfile(KEYCLOAK_ID, request));

            verify(sellerRepository, never()).save(any(Seller.class));
        }

        @Test
        void vendedorSuspendido_lanzaSellerSuspended() {
            Seller seller = aSeller().withKeycloakId(KEYCLOAK_ID).suspended().build();
            SellerUpdateRequest request = new SellerUpdateRequest("Tienda", null, null);
            when(sellerRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(seller));

            assertThrows(SellerSuspendedException.class,
                    () -> sellerProfileService.updateMyProfile(KEYCLOAK_ID, request));

            verify(sellerRepository, never()).save(any(Seller.class));
        }
    }

    @Nested
    class GetPublicProfile {

        @Test
        void perfilExistente_devuelvePerfil() {
            UUID sellerId = UUID.randomUUID();
            Seller seller = aSeller().withId(sellerId).build();
            SellerResponse expected = sellerResponseFor(seller);
            when(sellerRepository.findById(sellerId)).thenReturn(Optional.of(seller));
            when(sellerMapper.toResponse(seller)).thenReturn(expected);

            SellerResponse result = sellerProfileService.getPublicProfile(sellerId);

            assertEquals(expected, result);
        }

        @Test
        void perfilInexistente_lanzaSellerNotFound() {
            UUID sellerId = UUID.randomUUID();
            when(sellerRepository.findById(sellerId)).thenReturn(Optional.empty());

            assertThrows(SellerNotFoundException.class,
                    () -> sellerProfileService.getPublicProfile(sellerId));
        }
    }

    private static DataIntegrityViolationException dataIntegrityViolation(String constraint, String sqlState) {
        SQLException sql = new SQLException(
                "duplicate key value violates unique constraint \"" + constraint + "\"", sqlState);
        return new DataIntegrityViolationException("could not execute statement", sql);
    }

    private static SellerResponse sellerResponseFor(Seller seller) {
        return new SellerResponse(
                seller.getId(),
                seller.getKeycloakId(),
                seller.getStoreName(),
                seller.getEmail(),
                seller.getPhone(),
                seller.getDescription(),
                seller.getLogoUrl(),
                seller.getStatus(),
                seller.getCreatedAt(),
                seller.getUpdatedAt());
    }
}
