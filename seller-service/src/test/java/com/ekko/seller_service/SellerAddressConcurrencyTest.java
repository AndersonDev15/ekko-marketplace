package com.ekko.seller_service;

import com.ekko.seller_service.dto.request.SellerAddressRequest;
import com.ekko.seller_service.dto.response.SellerAddressResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerAddress;
import com.ekko.seller_service.repository.SellerAddressRepository;
import com.ekko.seller_service.service.SellerAddressService;
import com.ekko.seller_service.support.ConcurrencyRunner;
import com.ekko.seller_service.support.JwtTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


class SellerAddressConcurrencyTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private SellerAddressService sellerAddressService;

    @Autowired
    private SellerAddressRepository addressRepository;

    private Seller seller;

    @BeforeEach
    void seedSeller() {
        // addAddress uses validateCanEditProfile which requires PENDING_REVIEW
        seller = insertPendingReviewSeller(JwtTestUtils.SELLER_KEYCLOAK_ID);
    }

    @Test
    void addFirstAddress_concurrente_quedaUnaSolaPrimaria() {
        int threads = 8;

        List<SellerAddressResponse> results = ConcurrencyRunner.run(
                "primeras direcciones", threads,
                i -> () -> sellerAddressService.addAddress(seller.getId(), addressRequest("Dirección " + i)));

assertThat(results).hasSize(threads);

        long primaryInResults = results.stream()
                .filter(SellerAddressResponse::isPrimary)
                .count();
        assertThat(primaryInResults).isEqualTo(1);

        long primaryAddress = addressRepository.findBySellerId(seller.getId())
                .stream()
                .filter(SellerAddress::getPrimary)
                .count();
        assertThat(primaryAddress).isEqualTo(1);
        assertThat(addressRepository.countBySellerId(seller.getId())).isEqualTo(threads);
    }

    @Test
    void setPrimaryAddress_concurrente_quedaUnaSolaPrimaria() {
        int count = 4;
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(sellerAddressService.addAddress(seller.getId(), addressRequest("Dir " + i)).id());
        }
        // Change to PENDING_REVIEW for setPrimaryAddress (uses validateCanEditProfile)
        jdbcTemplate.update("UPDATE sellers SET status = 'PENDING_REVIEW' WHERE keycloak_id = ?", JwtTestUtils.SELLER_KEYCLOAK_ID);

        List<SellerAddressResponse> results = ConcurrencyRunner.run(
                "cambios de primary", 4,
                i -> () -> sellerAddressService.setPrimaryAddress(seller.getId(), ids.get(i)));

        assertThat(results).hasSize(4);

        long primaryAddress = addressRepository.findBySellerId(seller.getId())
                .stream()
                .filter(SellerAddress::getPrimary)
                .count();
        assertThat(primaryAddress).isEqualTo(1);
    }

    private SellerAddressRequest addressRequest(String line) {
        return new SellerAddressRequest(line, "Bogotá", "Cundinamarca", "Colombia", "110111");
    }
}