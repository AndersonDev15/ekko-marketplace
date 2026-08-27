package com.ekko.seller_service;

import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.repository.SellerRepository;
import com.ekko.seller_service.service.MinioService;
import com.ekko.seller_service.support.SellerTestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwtDecoderConfig.class)
@TestPropertySource(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
public abstract class AbstractPostgresIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16.14"))
            .withDatabaseName("seller_db")
            .withUsername("ekko")
            .withPassword("ekko123");

    static {
        POSTGRES.start();
        Runtime.getRuntime().addShutdownHook(new Thread(POSTGRES::stop));
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
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
                        Instant.now().plusSeconds(900)));
    }

    protected Seller insertSeller(SellerTestDataBuilder builder) {
        return sellerRepository.saveAndFlush(builder.build());
    }

    protected Seller insertActiveSeller(String keycloakId) {
        return insertSeller(SellerTestDataBuilder.aSeller().withKeycloakId(keycloakId).active());
    }

    protected Seller insertPendingReviewSeller(String keycloakId) {
        return insertSeller(SellerTestDataBuilder.aSeller().withKeycloakId(keycloakId).pendingReview());
    }

    protected void insertSellerMetrics(java.util.UUID sellerId) {
        jdbcTemplate.update("""
                INSERT INTO seller_metrics
                    (id, seller_id, total_sales, total_revenue, average_rating,
                     total_reviews, active_products, created_at, updated_at)
                VALUES (CAST(? AS uuid), CAST(? AS uuid), 0, 0, 0, 0, 0, now(), now())
                """, java.util.UUID.randomUUID().toString(), sellerId.toString());
    }
}
