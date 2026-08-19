package com.ekko.seller_service.mapper;

import com.ekko.seller_service.dto.response.SellerAddressResponse;
import com.ekko.seller_service.dto.response.SellerBankAccountResponse;
import com.ekko.seller_service.dto.response.SellerDocumentResponse;
import com.ekko.seller_service.dto.response.SellerMetricsResponse;
import com.ekko.seller_service.dto.response.SellerResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerAddress;
import com.ekko.seller_service.entity.SellerBankAccount;
import com.ekko.seller_service.entity.SellerDocument;
import com.ekko.seller_service.entity.SellerMetrics;
import com.ekko.seller_service.enums.BankAccountType;
import com.ekko.seller_service.enums.DocumentStatus;
import com.ekko.seller_service.enums.DocumentType;
import com.ekko.seller_service.enums.SellerStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.ekko.seller_service.support.AddressTestDataBuilder.anAddress;
import static com.ekko.seller_service.support.BankAccountTestDataBuilder.aBankAccount;
import static com.ekko.seller_service.support.DocumentTestDataBuilder.aDocument;
import static com.ekko.seller_service.support.SellerTestDataBuilder.aSeller;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SellerMapperTest {

    private final SellerMapper sellerMapper = Mappers.getMapper(SellerMapper.class);

    @Test
    void toResponse_mapeaTodosLosCampos() {
        UUID id = UUID.randomUUID();
        String keycloakId = "kc-test-0001";
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 2, 10, 0);
        Seller seller = aSeller()
                .withId(id)
                .withKeycloakId(keycloakId)
                .withStoreName("Mi tienda")
                .withEmail("seller@ekko.test")
                .withPhone("3001234567")
                .withDescription("Descripción")
                .withLogoUrl("https://logo.ekko.test/1")
                .active()
                .build();
        seller.setCreatedAt(createdAt);
        seller.setUpdatedAt(updatedAt);

        SellerResponse response = sellerMapper.toResponse(seller);

        assertEquals(id, response.id());
        assertEquals(keycloakId, response.keycloakId());
        assertEquals("Mi tienda", response.storeName());
        assertEquals("seller@ekko.test", response.email());
        assertEquals("3001234567", response.phone());
        assertEquals("Descripción", response.description());
        assertEquals("https://logo.ekko.test/1", response.logoUrl());
        assertEquals(SellerStatus.ACTIVE, response.status());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }

    @Test
    void toAddressResponse_mapeaTodosLosCampos() {
        Seller seller = aSeller().build();
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        SellerAddress address = anAddress()
                .withId(id)
                .withSeller(seller)
                .primary()
                .build();
        address.setCreatedAt(createdAt);

        SellerAddressResponse response = sellerMapper.toAddressResponse(address);

        assertEquals(id, response.id());
        assertEquals("Calle 1 #2-3", response.addressLine());
        assertEquals("Sincelejo", response.city());
        assertEquals("Sucre", response.state());
        assertEquals("Colombia", response.country());
        assertEquals("700001", response.postalCode());
        assertEquals(true, response.isPrimary());
        assertEquals(createdAt, response.createdAt());
    }

    @Test
    void toBankAccountResponse_mapeaTodosLosCampos() {
        Seller seller = aSeller().build();
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        SellerBankAccount account = aBankAccount()
                .withId(id)
                .withSeller(seller)
                .primary()
                .build();
        account.setCreatedAt(createdAt);

        SellerBankAccountResponse response = sellerMapper.toBankAccountResponse(account);

        assertEquals(id, response.id());
        assertEquals("Bancolombia", response.bankName());
        assertEquals(BankAccountType.SAVINGS, response.accountType());
        assertEquals("1234567890", response.accountNumber());
        assertEquals("Vendedor Ejemplo", response.accountHolder());
        assertEquals(true, response.isPrimary());
        assertEquals(createdAt, response.createdAt());
    }

    @Test
    void toDocumentResponse_mapeaTodosLosCampos() {
        Seller seller = aSeller().build();
        UUID id = UUID.randomUUID();
        LocalDateTime uploadedAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime reviewedAt = LocalDateTime.of(2026, 1, 2, 10, 0);
        SellerDocument document = aDocument()
                .withId(id)
                .withSeller(seller)
                .withDocumentType(DocumentType.RUT)
                .withStatus(DocumentStatus.APPROVED)
                .withUploadedAt(uploadedAt)
                .withReviewedAt(reviewedAt)
                .withNotes("Aprobado")
                .build();

        SellerDocumentResponse response = sellerMapper.toDocumentResponse(document);

        assertEquals(id, response.id());
        assertEquals(DocumentType.RUT, response.documentType());
        assertEquals(DocumentStatus.APPROVED, response.status());
        assertEquals(uploadedAt, response.uploadedAt());
        assertEquals(reviewedAt, response.reviewedAt());
        assertEquals("Aprobado", response.notes());
    }

    @Test
    void toMetricsResponse_mapeaTodosLosCampos() {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        SellerMetrics metrics = SellerMetrics.builder()
                .totalSales(5L)
                .totalRevenue(BigDecimal.valueOf(120.50))
                .averageRating(BigDecimal.valueOf(4.5))
                .totalReviews(10L)
                .activeProducts(3L)
                .build();
        metrics.setUpdatedAt(updatedAt);

        SellerMetricsResponse response = sellerMapper.toMetricsResponse(metrics);

        assertEquals(5L, response.totalSales());
        assertEquals(BigDecimal.valueOf(120.50), response.totalRevenue());
        assertEquals(BigDecimal.valueOf(4.5), response.averageRating());
        assertEquals(10L, response.totalReviews());
        assertEquals(3L, response.activeProducts());
        assertEquals(updatedAt, response.updatedAt());
    }
}
