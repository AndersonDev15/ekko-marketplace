package com.ekko.seller_service.support;

import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerAddress;

import java.util.UUID;

public final class AddressTestDataBuilder {

    private UUID id = UUID.randomUUID();
    private Seller seller;
    private String addressLine = "Calle 1 #2-3";
    private String city = "Sincelejo";
    private String state = "Sucre";
    private String country = "Colombia";
    private String postalCode = "700001";
    private Boolean primary = false;

    private AddressTestDataBuilder() {
    }

    public static AddressTestDataBuilder anAddress() {
        return new AddressTestDataBuilder();
    }

    public AddressTestDataBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public AddressTestDataBuilder withSeller(Seller seller) {
        this.seller = seller;
        return this;
    }

    public AddressTestDataBuilder withAddressLine(String addressLine) {
        this.addressLine = addressLine;
        return this;
    }

    public AddressTestDataBuilder withCity(String city) {
        this.city = city;
        return this;
    }

    public AddressTestDataBuilder withState(String state) {
        this.state = state;
        return this;
    }

    public AddressTestDataBuilder withCountry(String country) {
        this.country = country;
        return this;
    }

    public AddressTestDataBuilder withPostalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    public AddressTestDataBuilder primary() {
        this.primary = true;
        return this;
    }

    public AddressTestDataBuilder notPrimary() {
        this.primary = false;
        return this;
    }

    public SellerAddress build() {
        return SellerAddress.builder()
                .id(id)
                .seller(seller)
                .addressLine(addressLine)
                .city(city)
                .state(state)
                .country(country)
                .postalCode(postalCode)
                .primary(primary)
                .build();
    }
}
