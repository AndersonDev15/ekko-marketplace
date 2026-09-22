package com.ekko.seller_service;

import com.ekko.seller_service.dto.response.SellerResponse;
import com.ekko.seller_service.repository.SellerMetricsRepository;
import com.ekko.seller_service.repository.SellerRepository;
import com.ekko.seller_service.service.SellerProfileService;
import com.ekko.seller_service.support.ConcurrencyRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


class SellerProfileConcurrencyTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private SellerProfileService sellerProfileService;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private SellerMetricsRepository metricsRepository;

    @Test
    void getOrCreateMyProfile_concurrente_mismoKeycloakId_creaUnSoloPerfil() {
        int threads = 8;
        String keycloakId = UUID.randomUUID().toString();
        String email = "concurrente-" + UUID.randomUUID() + "@ekko.test";

        List<SellerResponse> results = ConcurrencyRunner.run(
                "getOrCreateMyProfile", threads,
                i -> () -> sellerProfileService.getOrCreateMyProfile(keycloakId, email));

        assertThat(results).hasSize(threads);
        assertThat(results).allSatisfy(r -> {
            assertThat(r.keycloakId()).isEqualTo(keycloakId);
            assertThat(r.email()).isEqualTo(email);
        });
        assertThat(sellerRepository.count()).isEqualTo(1);
        assertThat(metricsRepository.count()).isEqualTo(1);
    }
}
