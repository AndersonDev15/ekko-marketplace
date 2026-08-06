package com.ekko.seller_service.service;

import com.ekko.seller_service.dto.request.SellerDocumentRequest;
import com.ekko.seller_service.dto.response.SellerDocumentResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerDocument;
import com.ekko.seller_service.enums.DocumentStatus;
import com.ekko.seller_service.enums.DocumentType;
import com.ekko.seller_service.exception.DocumentAlreadyApprovedException;
import com.ekko.seller_service.exception.DocumentAlreadyPendingException;
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

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ekko.seller_service.support.DocumentTestDataBuilder.aDocument;
import static com.ekko.seller_service.support.SellerTestDataBuilder.aSeller;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerDocumentServiceTest {

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final DocumentType DOCUMENT_TYPE = DocumentType.ID_CARD;
    private static final String DOCUMENT_URL = "https://storage.ekko.test/docs/doc-123";

    private static final SellerDocumentRequest REQUEST = new SellerDocumentRequest(DOCUMENT_TYPE, DOCUMENT_URL);

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerDocumentRepository documentRepository;

    @Spy
    private SellerOperationValidator validator;

    @Mock
    private SellerMapper sellerMapper;

    @InjectMocks
    private SellerDocumentService sellerDocumentService;

    @Nested
    class AddDocument {

        @Test
        void documentoNuevo_guardaComoPending() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(false);
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.APPROVED)).thenReturn(false);
            when(documentRepository.saveAndFlush(any(SellerDocument.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sellerMapper.toDocumentResponse(any(SellerDocument.class)))
                    .thenAnswer(inv -> documentResponseFor(inv.getArgument(0)));

            SellerDocumentResponse result = sellerDocumentService.addDocument(SELLER_ID, REQUEST);

            assertEquals(DOCUMENT_TYPE, result.documentType());
            assertEquals(DocumentStatus.PENDING, result.status());
            ArgumentCaptor<SellerDocument> captor = ArgumentCaptor.forClass(SellerDocument.class);
            verify(documentRepository).saveAndFlush(captor.capture());
            SellerDocument created = captor.getValue();
            assertEquals(seller, created.getSeller());
            assertEquals(DOCUMENT_TYPE, created.getDocumentType());
            assertEquals(DOCUMENT_URL, created.getDocumentUrl());
            assertEquals(DocumentStatus.PENDING, created.getStatus());
        }

        @Test
        void documentoPendingExistente_lanzaDocumentAlreadyPending() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(true);

            assertThrows(DocumentAlreadyPendingException.class,
                    () -> sellerDocumentService.addDocument(SELLER_ID, REQUEST));
        }

        @Test
        void documentoApprovedExistente_lanzaDocumentAlreadyApproved() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(false);
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.APPROVED)).thenReturn(true);

            assertThrows(DocumentAlreadyApprovedException.class,
                    () -> sellerDocumentService.addDocument(SELLER_ID, REQUEST));
        }

        @Test
        void constraintUkSellerDocumentPending_lanzaDocumentAlreadyPending() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            DataIntegrityViolationException violation = dataIntegrityViolation("uk_seller_document_pending");
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(false);
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.APPROVED)).thenReturn(false);
            when(documentRepository.saveAndFlush(any(SellerDocument.class))).thenThrow(violation);

            assertThrows(DocumentAlreadyPendingException.class,
                    () -> sellerDocumentService.addDocument(SELLER_ID, REQUEST));
        }

        @Test
        void constraintUkSellerDocumentType_lanzaDocumentAlreadyPending() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            DataIntegrityViolationException violation = dataIntegrityViolation("uk_seller_document_type");
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(false);
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.APPROVED)).thenReturn(false);
            when(documentRepository.saveAndFlush(any(SellerDocument.class))).thenThrow(violation);

            assertThrows(DocumentAlreadyPendingException.class,
                    () -> sellerDocumentService.addDocument(SELLER_ID, REQUEST));
        }

        @Test
        void dataIntegrityViolation_conConstraintDistinta_relanzaExcepcion() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            SQLException sql = new SQLException(
                    "duplicate key value violates unique constraint \"uk_sellers_email\"", "23505");
            DataIntegrityViolationException violation = new DataIntegrityViolationException("stmt", sql);
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(false);
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.APPROVED)).thenReturn(false);
            when(documentRepository.saveAndFlush(any(SellerDocument.class))).thenThrow(violation);

            assertThrows(DataIntegrityViolationException.class,
                    () -> sellerDocumentService.addDocument(SELLER_ID, REQUEST));
        }

        @Test
        void dataIntegrityViolation_conCausaNoSql_relanzaExcepcion() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            DataIntegrityViolationException violation =
                    new DataIntegrityViolationException("stmt", new RuntimeException("boom"));
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.PENDING)).thenReturn(false);
            when(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(SELLER_ID, DOCUMENT_TYPE, DocumentStatus.APPROVED)).thenReturn(false);
            when(documentRepository.saveAndFlush(any(SellerDocument.class))).thenThrow(violation);

            assertThrows(DataIntegrityViolationException.class,
                    () -> sellerDocumentService.addDocument(SELLER_ID, REQUEST));
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

    private static DataIntegrityViolationException dataIntegrityViolation(String constraint) {
        SQLException sql = new SQLException(
                "duplicate key value violates unique constraint \"" + constraint + "\"", "23505");
        return new DataIntegrityViolationException("could not execute statement", sql);
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
}
