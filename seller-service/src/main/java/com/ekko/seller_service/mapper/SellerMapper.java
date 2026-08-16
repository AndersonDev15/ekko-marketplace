package com.ekko.seller_service.mapper;

import com.ekko.seller_service.dto.response.*;
import com.ekko.seller_service.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SellerMapper {

    SellerResponse toResponse(Seller seller);

    @Mapping(target = "isPrimary", source = "primary")
    SellerAddressResponse toAddressResponse(SellerAddress a);

    SellerBankAccountResponse toBankAccountResponse(SellerBankAccount b);

    SellerDocumentResponse toDocumentResponse(SellerDocument d);

    SellerMetricsResponse toMetricsResponse(SellerMetrics m);

}