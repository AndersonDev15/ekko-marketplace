package com.ekko.seller_service.config;

import com.ekko.seller_service.SellerServiceApplication;
import com.ekko.seller_service.TestJwtDecoderConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({
        SellerServiceApplication.class,
        TestJwtDecoderConfig.class
})
public class IntegrationTestConfig {
}