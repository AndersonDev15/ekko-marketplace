package com.ekko.seller_service.service;

import com.ekko.seller_service.dto.response.DocumentDownloadResponse;
import com.ekko.seller_service.dto.response.SellerDocumentResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerDocument;
import com.ekko.seller_service.enums.DocumentStatus;
import com.ekko.seller_service.enums.DocumentType;
import com.ekko.seller_service.exception.DocumentAlreadyApprovedException;
import com.ekko.seller_service.exception.DocumentAlreadyPendingException;
import com.ekko.seller_service.exception.SellerAlreadyActiveException;
import com.ekko.seller_service.exception.SellerDocumentNotFoundException;
import com.ekko.seller_service.exception.SellerSuspendedException;
import com.ekko.seller_service.mapper.SellerMapper;
import com.ekko.seller_service.repository.SellerDocumentRepository;
import com.ekko.seller_service.repository.SellerRepository;
import com.ekko.seller_service.support.SellerOperationValidator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ekko.seller_service.support.DocumentTestDataBuilder.aDocument;
import static com.ekko.seller_service.support.SellerTestDataBuilder.aSeller;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerDocumentServiceTest {

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final DocumentType DOCUMENT_TYPE = DocumentType.ID_CARD;
    private static final String OBJECT_KEY = "documents/abc-123/id_card.pdf";

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerDocumentRepository documentRepository;

    @Spy
    private SellerOperationValidator validator;

    @Mock
    private SellerMapper sellerMapper;

    @Mock
    private MinioService minioService;

    @InjectMocks
    private SellerDocumentService sellerDocumentService;

    @Nested
    class AddDocument {

        @Test
        void documentoNuevo_guardaComoPending() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(false);
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.APPROVED)).thenReturn(false);
            when(documentRepository.saveAndFlush(any(SellerDocument.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sellerMapper.toDocumentResponse(any(SellerDocument.class)))
                    .thenAnswer(inv -> documentResponseFor(inv.getArgument(0)));

            SellerDocumentResponse result = sellerDocumentService.addDocument(SELLER_ID, DOCUMENT_TYPE, OBJECT_KEY);

            assertEquals(DOCUMENT_TYPE, result.documentType());
            assertEquals(DocumentStatus.PENDING, result.status());
            ArgumentCaptor<SellerDocument> captor = ArgumentCaptor.forClass(SellerDocument.class);
            verify(documentRepository).saveAndFlush(captor.capture());
            SellerDocument created = captor.getValue();
            assertEquals(seller, created.getSeller());
            assertEquals(DOCUMENT_TYPE, created.getDocumentType());
            assertEquals(OBJECT_KEY, created.getObjectKey());
            assertEquals(DocumentStatus.PENDING, created.getStatus());
        }

        @Test
        void vendedorSuspendido_lanzaSellerSuspended() {
            Seller seller = aSeller().withId(SELLER_ID).suspended().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));

            assertThrows(SellerSuspendedException.class,
                    () -> sellerDocumentService.addDocument(SELLER_ID, DOCUMENT_TYPE, OBJECT_KEY));
        }

        @Test
        void documentoPendingExistente_lanzaDocumentAlreadyPending() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(true);

            assertThrows(DocumentAlreadyPendingException.class,
                    () -> sellerDocumentService.addDocument(SELLER_ID, DOCUMENT_TYPE, OBJECT_KEY));
        }

        @Test
        void documentoApprovedExistente_lanzaDocumentAlreadyApproved() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(false);
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.APPROVED)).thenReturn(true);

            assertThrows(DocumentAlreadyApprovedException.class,
                    () -> sellerDocumentService.addDocument(SELLER_ID, DOCUMENT_TYPE, OBJECT_KEY));
        }

        @Test
        void constraintUkSellerDocumentPending_lanzaDocumentAlreadyPending() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            DataIntegrityViolationException violation = dataIntegrityViolation("uk_seller_document_pending");
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(false);
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.APPROVED)).thenReturn(false);
            when(documentRepository.saveAndFlush(any(SellerDocument.class))).thenThrow(violation);

            assertThrows(DocumentAlreadyPendingException.class,
                    () -> sellerDocumentService.addDocument(SELLER_ID, DOCUMENT_TYPE, OBJECT_KEY));
        }

        @Test
        void constraintUkSellerDocumentType_lanzaDocumentAlreadyPending() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            DataIntegrityViolationException violation = dataIntegrityViolation("uk_seller_document_type");
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(false);
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.APPROVED)).thenReturn(false);
            when(documentRepository.saveAndFlush(any(SellerDocument.class))).thenThrow(violation);

            assertThrows(DocumentAlreadyPendingException.class,
                    () -> sellerDocumentService.addDocument(SELLER_ID, DOCUMENT_TYPE, OBJECT_KEY));
        }

        @Test
        void dataIntegrityViolation_conConstraintDistinta_relanzaExcepcion() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            SQLException sql = new SQLException(
                    "duplicate key value violates unique constraint \"uk_sellers_email\"", "23505");
            DataIntegrityViolationException violation = new DataIntegrityViolationException("stmt", sql);
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(false);
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.APPROVED)).thenReturn(false);
            when(documentRepository.saveAndFlush(any(SellerDocument.class))).thenThrow(violation);

            assertThrows(DataIntegrityViolationException.class,
                    () -> sellerDocumentService.addDocument(SELLER_ID, DOCUMENT_TYPE, OBJECT_KEY));
        }

        @Test
        void dataIntegrityViolation_conCausaNoSql_relanzaExcepcion() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            DataIntegrityViolationException violation =
                    new DataIntegrityViolationException("stmt", new RuntimeException("boom"));
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(false);
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.APPROVED)).thenReturn(false);
            when(documentRepository.saveAndFlush(any(SellerDocument.class))).thenThrow(violation);

            assertThrows(DataIntegrityViolationException.class,
                    () -> sellerDocumentService.addDocument(SELLER_ID, DOCUMENT_TYPE, OBJECT_KEY));
        }
    }

    @Nested
    class UploadDocument {

        private final MockMultipartFile FILE = new MockMultipartFile(
                "file", "id_card.pdf", "application/pdf", new byte[]{1, 2, 3});

        @Test
        void documentoValido_subirAMinioYPersistir() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(false);
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.APPROVED)).thenReturn(false);
            when(minioService.upload(FILE)).thenReturn(OBJECT_KEY);
            when(documentRepository.saveAndFlush(any(SellerDocument.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sellerMapper.toDocumentResponse(any(SellerDocument.class)))
                    .thenAnswer(inv -> documentResponseFor(inv.getArgument(0)));

            SellerDocumentResponse result = sellerDocumentService.uploadDocument(SELLER_ID, DOCUMENT_TYPE, FILE);

            assertEquals(DOCUMENT_TYPE, result.documentType());
            assertEquals(DocumentStatus.PENDING, result.status());
            ArgumentCaptor<SellerDocument> captor = ArgumentCaptor.forClass(SellerDocument.class);
            verify(documentRepository).saveAndFlush(captor.capture());
            SellerDocument created = captor.getValue();
            assertEquals(OBJECT_KEY, created.getObjectKey());
            assertEquals(DocumentStatus.PENDING, created.getStatus());
        }

        @Test
        void vendedorSuspendido_lanzaSellerSuspended() {
            Seller seller = aSeller().withId(SELLER_ID).suspended().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));

            assertThrows(SellerSuspendedException.class,
                    () -> sellerDocumentService.uploadDocument(SELLER_ID, DOCUMENT_TYPE, FILE));

            verifyNoInteractions(minioService);
        }

        @Test
        void documentoPendingExistente_lanzaSinSubirAMinio() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(true);

            assertThrows(DocumentAlreadyPendingException.class,
                    () -> sellerDocumentService.uploadDocument(SELLER_ID, DOCUMENT_TYPE, FILE));

            verifyNoInteractions(minioService);
        }

        @Test
        void documentoApprovedExistente_lanzaSinSubirAMinio() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(false);
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.APPROVED)).thenReturn(true);

            assertThrows(DocumentAlreadyApprovedException.class,
                    () -> sellerDocumentService.uploadDocument(SELLER_ID, DOCUMENT_TYPE, FILE));

            verifyNoInteractions(minioService);
        }

        @Test
        void vendedorSuspendido_lanzaSinSubirAMinio() {
            Seller seller = aSeller().withId(SELLER_ID).suspended().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));

            assertThrows(SellerSuspendedException.class,
                    () -> sellerDocumentService.uploadDocument(SELLER_ID, DOCUMENT_TYPE, FILE));

            verifyNoInteractions(minioService);
        }

        @Test
        void conflictoAlPersistir_borraObjetoYRelanza() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(false);
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.APPROVED)).thenReturn(false);
            when(minioService.upload(FILE)).thenReturn(OBJECT_KEY);
            when(documentRepository.saveAndFlush(any(SellerDocument.class)))
                    .thenThrow(dataIntegrityViolation("uk_seller_document_pending"));

            assertThrows(DocumentAlreadyPendingException.class,
                    () -> sellerDocumentService.uploadDocument(SELLER_ID, DOCUMENT_TYPE, FILE));

            verify(minioService).delete(OBJECT_KEY);
        }

        @Test
        void falloDePersistenciaPorCualquierMotivo_borraObjetoYRelanza() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(false);
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.APPROVED)).thenReturn(false);
            when(minioService.upload(FILE)).thenReturn(OBJECT_KEY);
            when(documentRepository.saveAndFlush(any(SellerDocument.class)))
                    .thenThrow(new RuntimeException("database connection lost"));

            assertThrows(RuntimeException.class,
                    () -> sellerDocumentService.uploadDocument(SELLER_ID, DOCUMENT_TYPE, FILE));

            verify(minioService).delete(OBJECT_KEY);
        }
    }

    @Nested
    class GetMyDocuments {

        @Test
        void conDocumentos_devuelveLista() {
            Seller seller = aSeller().withId(SELLER_ID).build();
            SellerDocument first = aDocument().withSeller(seller).withDocumentType(DocumentType.ID_CARD).build();
            SellerDocument second = aDocument().withSeller(seller).withDocumentType(DocumentType.RUT).build();
            when(documentRepository.findBySellerId(SELLER_ID)).thenReturn(List.of(first, second));
            when(sellerMapper.toDocumentResponse(any(SellerDocument.class)))
                    .thenAnswer(inv -> documentResponseFor(inv.getArgument(0)));

            List<SellerDocumentResponse> result = sellerDocumentService.getMyDocuments(SELLER_ID);

            assertEquals(2, result.size());
            assertEquals(List.of(DocumentType.ID_CARD, DocumentType.RUT),
                    result.stream().map(SellerDocumentResponse::documentType).toList());
        }

        @Test
        void vacio_devuelveListaVacia() {
            when(documentRepository.findBySellerId(SELLER_ID)).thenReturn(List.of());

            List<SellerDocumentResponse> result = sellerDocumentService.getMyDocuments(SELLER_ID);

            assertEquals(List.of(), result);
        }
    }

    @Nested
    class GetDownloadUrl {

        @Test
        void documentoPropio_devuelveUrlConExpiracion() {
            Seller seller = aSeller().withId(SELLER_ID).build();
            UUID documentId = UUID.randomUUID();
            Instant expiresAt = Instant.now().plusSeconds(900);
            when(documentRepository.findByIdAndSellerId(documentId, SELLER_ID))
                    .thenReturn(Optional.of(aDocument()
                            .withId(documentId)
                            .withSeller(seller)
                            .withObjectKey(OBJECT_KEY)
                            .build()));
            when(minioService.generatePresignedUrl(OBJECT_KEY))
                    .thenReturn(new MinioService.PresignedUrl(
                            "http://localhost:9000/ekko-documents/documents/abc-123/id_card.pdf?X-Amz-Signature=xyz",
                            expiresAt));

            DocumentDownloadResponse result = sellerDocumentService.getDownloadUrl(SELLER_ID, documentId);

            assertEquals(documentId, result.documentId());
            assertEquals("http://localhost:9000/ekko-documents/documents/abc-123/id_card.pdf?X-Amz-Signature=xyz",
                    result.downloadUrl());
            assertEquals(expiresAt, result.expiresAt());
        }

        @Test
        void documentoDeOtroVendedor_lanzaNotFound() {
            UUID documentId = UUID.randomUUID();
            when(documentRepository.findByIdAndSellerId(documentId, SELLER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(SellerDocumentNotFoundException.class,
                    () -> sellerDocumentService.getDownloadUrl(SELLER_ID, documentId));
        }
    }

    private static DataIntegrityViolationException dataIntegrityViolation(String constraint) {
        SQLException sql = new SQLException(
                "duplicate key value violates unique constraint \"" + constraint + "\"", "23505");
        return new DataIntegrityViolationException("could not execute statement", sql);
    }

    private static SellerDocumentResponse documentResponseFor(SellerDocument document) {
        return new SellerDocumentResponse(
                document.getId(),
                document.getDocumentType(),
                document.getStatus(),
                document.getUploadedAt(),
                document.getReviewedAt(),
                document.getNotes());
    }
}