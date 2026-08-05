package com.ekko.seller_service.service;

import com.ekko.seller_service.support.PrimaryEntityPolicy;
import com.ekko.seller_service.support.SellerOperationValidator;
import com.ekko.seller_service.dto.request.SellerAddressRequest;
import com.ekko.seller_service.dto.response.SellerAddressResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerAddress;
import com.ekko.seller_service.exception.*;
import com.ekko.seller_service.mapper.SellerMapper;
import com.ekko.seller_service.repository.SellerAddressRepository;
import com.ekko.seller_service.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerAddressService {

    private final SellerAddressRepository addressRepository;
    private final SellerRepository sellerRepository;
    private final PrimaryEntityPolicy primaryPolicy;
    private final SellerOperationValidator validator;
    private final SellerMapper sellerMapper;

    @Transactional
    public SellerAddressResponse addAddress(UUID sellerId, SellerAddressRequest request) {
        Seller seller = sellerRepository.findByIdForUpdate(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));

        validator.validateCanOperate(seller);

        boolean isFirst = !addressRepository.existsBySellerId(sellerId);

        SellerAddress address = SellerAddress.builder()
                .seller(seller)
                .addressLine(request.addressLine())
                .city(request.city())
                .state(request.state())
                .country(request.country())
                .postalCode(request.postalCode())
                .primary(isFirst)
                .build();

        return sellerMapper.toAddressResponse(addressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public List<SellerAddressResponse> getMyAddresses(UUID sellerId) {
        return addressRepository.findBySellerId(sellerId)
                .stream()
                .map(sellerMapper::toAddressResponse)
                .toList();
    }

    @Transactional
    public SellerAddressResponse updateAddress(UUID sellerId, UUID addressId, SellerAddressRequest request) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));

        validator.validateCanOperate(seller);

        SellerAddress address = addressRepository
                .findByIdAndSellerId(addressId, sellerId)
                .orElseThrow(() -> new SellerAddressNotFoundException(addressId, sellerId));

        address.setAddressLine(request.addressLine());
        address.setCity(request.city());
        address.setState(request.state());
        address.setCountry(request.country());
        address.setPostalCode(request.postalCode());

        return sellerMapper.toAddressResponse(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(UUID sellerId, UUID addressId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));

        validator.validateCanOperate(seller);

        SellerAddress address = addressRepository
                .findByIdAndSellerId(addressId, sellerId)
                .orElseThrow(() -> new SellerAddressNotFoundException(addressId, sellerId));

        long totalAddresses = addressRepository.countBySellerId(sellerId);

        if (totalAddresses == 1) {
            throw new LastAddressDeleteException(addressId, sellerId);
        }
        if (Boolean.TRUE.equals(address.getPrimary())) {
            throw new PrimaryAddressDeleteException();
        }

        addressRepository.delete(address);
    }


    @Transactional
    public SellerAddressResponse setPrimaryAddress(UUID sellerId, UUID addressId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));

        validator.validateCanOperate(seller);

        SellerAddress address = addressRepository
                .findByIdAndSellerId(addressId, sellerId)
                .orElseThrow(() -> new SellerAddressNotFoundException(addressId, sellerId));

        if (Boolean.TRUE.equals(address.getPrimary())) {
            return sellerMapper.toAddressResponse(address);
        }

        primaryPolicy.promote(
                () -> addressRepository.clearPrimaryBySellerId(sellerId),
                () -> address.setPrimary(true)
        );

        return sellerMapper.toAddressResponse(addressRepository.save(address));
    }


}