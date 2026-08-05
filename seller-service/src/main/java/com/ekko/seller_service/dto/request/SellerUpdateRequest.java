package com.ekko.seller_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerUpdateRequest(
        @NotBlank(message = "El nombre de la tienda es obligatorio")
        @Size(max = 255)
        String storeName,

        @Size(max = 20)
        String phone,

        String description
) {}
