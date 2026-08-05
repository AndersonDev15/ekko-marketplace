package com.ekko.seller_service.support;

import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.exception.SellerNotFoundException;
import com.ekko.seller_service.repository.SellerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ekko.seller_service.support.SellerTestDataBuilder.aSeller;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerResolverTest {

    private static final String KEYCLOAK_ID = "kc-test-0001";

    @Mock
    private SellerRepository sellerRepository;

    @InjectMocks
    private SellerResolver sellerResolver;

    @Test
    void resolve_vendedorExistente_devuelveSeller() {
        Seller seller = aSeller().withKeycloakId(KEYCLOAK_ID).build();
        when(sellerRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(seller));

        Seller result = sellerResolver.resolve(KEYCLOAK_ID);

        assertEquals(seller, result);
    }

    @Test
    void resolve_vendedorInexistente_lanzaSellerNotFound() {
        when(sellerRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.empty());

        assertThrows(SellerNotFoundException.class, () -> sellerResolver.resolve(KEYCLOAK_ID));
    }
}
