package com.ekko.order_service.infrastructure.persistence.adapter.out.product;

import com.ekko.order_service.infrastructure.config.OAuth2FeignConfig;
import com.ekko.order_service.infrastructure.persistence.adapter.out.product.dto.ProductVariantInfo;
import com.ekko.order_service.infrastructure.persistence.adapter.out.product.dto.StockReservationItem;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "product-service",
        url = "${services.product-service.url}",
        configuration = OAuth2FeignConfig.class
)
public interface ProductServiceClient {

    @GetMapping("/internal/variants")
    List<ProductVariantInfo> getVariantsInfo(
            @RequestParam("variantIds") List<UUID> variantIds
    );

    @PostMapping("/internal/inventory/reserve")
    void reserveStock(
            @RequestBody List<StockReservationItem> items
    );
}
