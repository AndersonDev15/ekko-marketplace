package com.ekko.seller_service.repository;

import com.ekko.seller_service.AbstractPostgresRepositoryTest;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerDocument;
import com.ekko.seller_service.enums.DocumentStatus;
import com.ekko.seller_service.enums.DocumentType;
import com.ekko.seller_service.enums.SellerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SellerDocumentRepositoryTest extends AbstractPostgresRepositoryTest {

    @Autowired
    private SellerDocumentRepository documentRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Test
    void findBySellerId_devuelveSoloDocumentosDelVendedor() {
        Seller sellerA = saveSeller("kc-001", "a@ekko.test");
        Seller sellerB = saveSeller("kc-002", "b@ekko.test");
        documentRepository.saveAndFlush(document(sellerA, DocumentType.ID_CARD));
        documentRepository.saveAndFlush(document(sellerA, DocumentType.RUT));
        documentRepository.saveAndFlush(document(sellerB, DocumentType.ID_CARD));

        List<SellerDocument> result = documentRepository.findBySellerId(sellerA.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(d -> d.getSeller().getId().equals(sellerA.getId()));
    }

    @Test
    void existsBySellerIdAndDocumentTypeAndStatus_detectaPorEstado() {
        Seller seller = saveSeller("kc-001", "a@ekko.test");
        documentRepository.saveAndFlush(document(seller, DocumentType.ID_CARD));

        assertThat(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(
                seller.getId(), DocumentType.ID_CARD, DocumentStatus.PENDING)).isTrue();
        assertThat(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(
                seller.getId(), DocumentType.ID_CARD, DocumentStatus.APPROVED)).isFalse();
        assertThat(documentRepository.existsBySellerIdAndDocumentTypeAndStatus(
                seller.getId(), DocumentType.RUT, DocumentStatus.PENDING)).isFalse();
    }

    @Test
    void constraintUkSellerDocumentType_mismoTipoMismoVendedor_lanzaViolacion() {
        Seller seller = saveSeller("kc-001", "a@ekko.test");
        documentRepository.saveAndFlush(document(seller, DocumentType.ID_CARD));

        assertThatThrownBy(() -> documentRepository.saveAndFlush(
                document(seller, DocumentType.ID_CARD, DocumentStatus.APPROVED)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void tiposDiferentesParaElMismoVendedor_sonPermitidos() {
        Seller seller = saveSeller("kc-001", "a@ekko.test");
        documentRepository.saveAndFlush(document(seller, DocumentType.ID_CARD));
        documentRepository.saveAndFlush(document(seller, DocumentType.RUT));

        assertThat(documentRepository.findBySellerId(seller.getId())).hasSize(2);
    }

    @Test
    void constraintUkSellerDocumentPending_dosPendingMismoTipo_lanzaViolacion() {
        Seller seller = saveSeller("kc-001", "a@ekko.test");
        documentRepository.saveAndFlush(document(seller, DocumentType.ID_CARD));

        assertThatThrownBy(() -> documentRepository.saveAndFlush(
                document(seller, DocumentType.ID_CARD, DocumentStatus.PENDING)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void constraintFkVendedorInexistente_lanzaViolacion() {
        Seller ghost = Seller.builder().id(UUID.randomUUID()).build();

        assertThatThrownBy(() -> documentRepository.saveAndFlush(document(ghost, DocumentType.ID_CARD)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void constraintsYIndicesUnicosExistenEnEsquema() {
        Integer constraints = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_constraint WHERE conname IN ('uk_seller_document_type')", Integer.class);
        Integer pendingIndex = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'uk_seller_document_pending'",
                Integer.class);

        assertThat(constraints).isEqualTo(1);
        assertThat(pendingIndex).isEqualTo(1);
    }

    private Seller saveSeller(String keycloakId, String email) {
        return sellerRepository.saveAndFlush(Seller.builder()
                .keycloakId(keycloakId)
                .storeName("Mi tienda")
                .email(email)
                .status(SellerStatus.ACTIVE)
                .build());
    }

    private static SellerDocument document(Seller seller, DocumentType type) {
        return document(seller, type, DocumentStatus.PENDING);
    }

    private static SellerDocument document(Seller seller, DocumentType type, DocumentStatus status) {
        return SellerDocument.builder()
                .seller(seller)
                .documentType(type)
                .documentUrl("https://storage.ekko.test/docs/" + UUID.randomUUID())
                .status(status)
                .build();
    }
}
