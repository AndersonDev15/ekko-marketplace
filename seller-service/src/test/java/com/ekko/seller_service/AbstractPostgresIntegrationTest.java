package com.ekko.seller_service;

import com.ekko.seller_service.config.IntegrationTestConfig;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.repository.SellerRepository;
import com.ekko.seller_service.service.MinioService;
import com.ekko.seller_service.support.SellerTestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration(classes = IntegrationTestConfig.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
public abstract class AbstractPostgresIntegrationTest {

    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("seller_db")
                    .withUsername("ekko")
                    .withPassword("ekko123");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> "false");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "32");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected SellerRepository sellerRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected RabbitTemplate rabbitTemplate;

    @MockitoBean
    protected MinioService minioService;

    @MockitoBean
    protected MinioClient minioClient;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE order_confirmations,
                               product_events,
                               review_confirmations,
                               seller_documents,
                               seller_bank_accounts,
                               seller_addresses,
                               seller_metrics,
                               sellers
                RESTART IDENTITY CASCADE
                """);

        when(minioService.upload(any(MultipartFile.class)))
                .thenReturn("documents/abc-123/id_card.pdf");

        when(minioService.generatePresignedUrl(anyString()))
                .thenReturn(new MinioService.PresignedUrl(
                        "http://localhost:9000/ekko-documents/documents/abc-123/id_card.pdf?X-Amz-Signature=test",
                        Instant.now().plusSeconds(900)
                ));
    }

    protected Seller insertSeller(SellerTestDataBuilder builder) {
        return sellerRepository.saveAndFlush(builder.build());
    }

    protected Seller insertActiveSeller(String keycloakId) {
        return insertSeller(
                SellerTestDataBuilder.aSeller()
                        .withKeycloakId(keycloakId)
                        .active()
        );
    }

    protected Seller insertPendingReviewSeller(String keycloakId) {
        return insertSeller(
                SellerTestDataBuilder.aSeller()
                        .withKeycloakId(keycloakId)
                        .pendingReview()
        );
    }

    protected void insertSellerMetrics(UUID sellerId) {
        jdbcTemplate.update("""
                INSERT INTO seller_metrics
                    (id, seller_id, total_sales, total_revenue, average_rating,
                     total_reviews, active_products, created_at, updated_at)
                VALUES (CAST(? AS uuid), CAST(? AS uuid), 0, 0, 0, 0, 0, now(), now())
                """,
                UUID.randomUUID().toString(),
                sellerId.toString()
        );
    }
}