package com.ekko.seller_service.service;

import com.ekko.seller_service.dto.request.ReviewDocumentRequest;
import com.ekko.seller_service.dto.request.UpdateSellerStatusRequest;
import com.ekko.seller_service.dto.response.SellerDetailResponse;
import com.ekko.seller_service.dto.response.SellerDocumentResponse;
import com.ekko.seller_service.dto.response.SellerMetricsResponse;
import com.ekko.seller_service.dto.response.SellerResponse;
import com.ekko.seller_service.dto.response.SellerSummaryResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerDocument;
import com.ekko.seller_service.entity.SellerMetrics;
import com.ekko.seller_service.enums.DocumentStatus;
import com.ekko.seller_service.enums.DocumentType;
import com.ekko.seller_service.enums.SellerStatus;
import com.ekko.seller_service.exception.DocumentAlreadyReviewedException;
import com.ekko.seller_service.exception.InvalidStatusTransitionException;
import com.ekko.seller_service.exception.SellerDocumentNotFoundException;
import com.ekko.seller_service.exception.SellerMetricsNotFoundException;
import com.ekko.seller_service.exception.SellerNotFoundException;
import com.ekko.seller_service.mapper.SellerMapper;
import com.ekko.seller_service.messaging.SellerEventPublisher;
import com.ekko.seller_service.messaging.dto.SellerDocumentReviewEvent;
import com.ekko.seller_service.messaging.dto.SellerStatusChangedEvent;
import com.ekko.seller_service.repository.SellerDocumentRepository;
import com.ekko.seller_service.repository.SellerMetricsRepository;
import com.ekko.seller_service.repository.SellerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ekko.seller_service.support.DocumentTestDataBuilder.aDocument;
import static com.ekko.seller_service.support.SellerTestDataBuilder.aSeller;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSellerServiceTest {

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID DOCUMENT_ID = UUID.randomUUID();

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerDocumentRepository sellerDocumentRepository;

    @Mock
    private SellerMetricsRepository sellerMetricsRepository;

    @Mock
    private SellerMapper sellerMapper;

    @Mock
    private SellerEventPublisher sellerEventPublisher;

    @InjectMocks
    private AdminSellerService adminSellerService;

    @Test
    void getAllSellers_devuelvePaginaResumida() {
        SellerStatus status = SellerStatus.ACTIVE;
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        Pageable pageable = Pageable.unpaged();
        Seller seller = aSeller().active().build();
        when(sellerRepository.findAllFiltered(status, from, to, pageable))
                .thenReturn(new PageImpl<>(List.of(seller)));

        Page<SellerSummaryResponse> result = adminSellerService.getAllSellers(status, from, to, pageable);

        assertEquals(1, result.getTotalElements());
        SellerSummaryResponse summary = result.getContent().get(0);
        assertEquals(seller.getId(), summary.id());
        assertEquals(seller.getStoreName(), summary.storeName());
        assertEquals(seller.getEmail(), summary.email());
        assertEquals(seller.getStatus(), summary.status());
    }

    @Test
    void getSellerDetail_vendedorExistente_devuelveDetalle() {
        Seller seller = aSeller().withId(SELLER_ID).build();
        SellerDocument document = aDocument().withSeller(seller).build();
        SellerMetrics metrics = SellerMetrics.builder().totalSales(5L).build();
        when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
        when(sellerDocumentRepository.findBySellerId(SELLER_ID)).thenReturn(List.of(document));
        when(sellerMetricsRepository.findBySellerId(SELLER_ID)).thenReturn(Optional.of(metrics));
        when(sellerMapper.toDocumentResponse(document)).thenReturn(documentResponseFor(document));
        when(sellerMapper.toMetricsResponse(metrics)).thenReturn(metricsResponseFor(metrics));

        SellerDetailResponse result = adminSellerService.getSellerDetail(SELLER_ID);

        assertEquals(SELLER_ID, result.id());
        assertEquals(seller.getKeycloakId(), result.keycloakId());
        assertEquals(1, result.documents().size());
        assertEquals(documentResponseFor(document), result.documents().get(0));
        assertEquals(metricsResponseFor(metrics), result.metrics());
    }

    @Test
    void getSellerDetail_vendedorInexistente_lanzaSellerNotFound() {
        when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.empty());

        assertThrows(SellerNotFoundException.class,
                () -> adminSellerService.getSellerDetail(SELLER_ID));
    }

    @Test
    void getSellerDetail_sinMetricas_lanzaMetricsNotFound() {
        Seller seller = aSeller().withId(SELLER_ID).build();
        when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
        when(sellerDocumentRepository.findBySellerId(SELLER_ID)).thenReturn(List.of());
        when(sellerMetricsRepository.findBySellerId(SELLER_ID)).thenReturn(Optional.empty());

        assertThrows(SellerMetricsNotFoundException.class,
                () -> adminSellerService.getSellerDetail(SELLER_ID));
    }

    @Test
    void updateSellerStatus_transicionValida_actualizaEstado() {
        Seller seller = aSeller().withId(SELLER_ID).active().build();
        UpdateSellerStatusRequest request = new UpdateSellerStatusRequest(SellerStatus.SUSPENDED);
        when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
        when(sellerRepository.save(seller)).thenReturn(seller);
        when(sellerMapper.toResponse(seller))
                .thenAnswer(inv -> sellerResponseFor(seller));

        SellerResponse result = adminSellerService.updateSellerStatus(SELLER_ID, request);

        assertEquals(SellerStatus.SUSPENDED, seller.getStatus());
        assertEquals(SellerStatus.SUSPENDED, result.status());
        assertEquals(SELLER_ID, result.id());
        verify(sellerRepository).save(seller);

        ArgumentCaptor<SellerStatusChangedEvent> captor = ArgumentCaptor.forClass(SellerStatusChangedEvent.class);
        verify(sellerEventPublisher).publishSellerStatusChanged(captor.capture());
        SellerStatusChangedEvent event = captor.getValue();
        assertEquals(SELLER_ID, event.sellerId());
        assertEquals(seller.getKeycloakId(), event.keycloakId());
        assertEquals(SellerStatus.ACTIVE, event.previousStatus());
        assertEquals(SellerStatus.SUSPENDED, event.newStatus());
        assertNotNull(event.changedAt());
    }

    @Test
    void updateSellerStatus_transicionInvalida_lanzaInvalidTransition() {
        Seller seller = aSeller().withId(SELLER_ID).active().build();
        UpdateSellerStatusRequest request = new UpdateSellerStatusRequest(SellerStatus.PENDING_REVIEW);
        when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));

        assertThrows(InvalidStatusTransitionException.class,
                () -> adminSellerService.updateSellerStatus(SELLER_ID, request));

        verify(sellerRepository, never()).save(any(Seller.class));
        verify(sellerEventPublisher, never()).publishSellerStatusChanged(any());
    }

    @Test
    void updateSellerStatus_vendedorInexistente_lanzaSellerNotFound() {
        UpdateSellerStatusRequest request = new UpdateSellerStatusRequest(SellerStatus.SUSPENDED);
        when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.empty());

        assertThrows(SellerNotFoundException.class,
                () -> adminSellerService.updateSellerStatus(SELLER_ID, request));

        verify(sellerEventPublisher, never()).publishSellerStatusChanged(any());
    }

    @Test
    void reviewDocument_documentoPending_actualizaRevision() {
        Seller seller = aSeller().withId(SELLER_ID).build();
        SellerDocument document = aDocument().withId(DOCUMENT_ID).withSeller(seller).pending().build();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("admin-kc-1").build();
        ReviewDocumentRequest request = new ReviewDocumentRequest(DocumentStatus.APPROVED, "ok");
        when(sellerDocumentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
        when(sellerDocumentRepository.save(document)).thenReturn(document);
        when(sellerMapper.toDocumentResponse(document))
                .thenAnswer(inv -> documentResponseFor(document));

        SellerDocumentResponse result = adminSellerService.reviewDocument(DOCUMENT_ID, request, jwt);

        assertEquals(DocumentStatus.APPROVED, result.status());
        assertEquals(DocumentStatus.APPROVED, document.getStatus());
        assertEquals("ok", document.getNotes());
        assertEquals("admin-kc-1", document.getReviewedBy());
        assertNotNull(document.getReviewedAt());
        verify(sellerDocumentRepository).save(document);

        ArgumentCaptor<SellerDocumentReviewEvent> captor = ArgumentCaptor.forClass(SellerDocumentReviewEvent.class);
        verify(sellerEventPublisher).publishSellerDocumentReview(captor.capture());
        SellerDocumentReviewEvent event = captor.getValue();
        assertEquals(SELLER_ID, event.sellerId());
        assertEquals(DOCUMENT_ID, event.documentId());
        assertEquals(DocumentType.ID_CARD, event.documentType());
        assertEquals(DocumentStatus.APPROVED, event.reviewStatus());
        assertEquals("ok", event.notes());
        assertNotNull(event.reviewedAt());
    }

    @Test
    void reviewDocument_documentoNoPending_lanzaDocumentAlreadyReviewed() {
        SellerDocument document = aDocument().withId(DOCUMENT_ID).approved().build();
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("admin-kc-1").build();
        ReviewDocumentRequest request = new ReviewDocumentRequest(DocumentStatus.APPROVED, null);
        when(sellerDocumentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));

        assertThrows(DocumentAlreadyReviewedException.class,
                () -> adminSellerService.reviewDocument(DOCUMENT_ID, request, jwt));

        verify(sellerDocumentRepository, never()).save(any(SellerDocument.class));
        verify(sellerEventPublisher, never()).publishSellerDocumentReview(any());
    }

    @Test
    void reviewDocument_documentoInexistente_lanzaDocumentNotFound() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("admin-kc-1").build();
        ReviewDocumentRequest request = new ReviewDocumentRequest(DocumentStatus.APPROVED, null);
        when(sellerDocumentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.empty());

        assertThrows(SellerDocumentNotFoundException.class,
                () -> adminSellerService.reviewDocument(DOCUMENT_ID, request, jwt));

        verify(sellerEventPublisher, never()).publishSellerDocumentReview(any());
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

    private static SellerDocumentResponse documentResponseFor(SellerDocument document) {
        return new SellerDocumentResponse(
                document.getId(),
                document.getDocumentType(),
                document.getDocumentUrl(),
                document.getStatus(),
                document.getUploadedAt(),
                document.getReviewedAt(),
                document.getNotes());
    }

    private static SellerMetricsResponse metricsResponseFor(SellerMetrics metrics) {
        return new SellerMetricsResponse(
                metrics.getTotalSales(),
                metrics.getTotalRevenue(),
                metrics.getAverageRating(),
                metrics.getTotalReviews(),
                metrics.getActiveProducts(),
                metrics.getUpdatedAt());
    }
}
