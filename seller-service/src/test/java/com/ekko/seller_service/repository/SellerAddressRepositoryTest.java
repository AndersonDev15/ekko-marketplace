package com.ekko.seller_service.repository;

import com.ekko.seller_service.AbstractPostgresRepositoryTest;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerAddress;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SellerAddressRepositoryTest extends AbstractPostgresRepositoryTest {

    @Autowired
    private SellerAddressRepository addressRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Test
    void findBySellerId_devuelveSoloDireccionesDelVendedor() {
        Seller sellerA = saveSeller("kc-001", "a@ekko.test");
        Seller sellerB = saveSeller("kc-002", "b@ekko.test");
        addressRepository.saveAndFlush(address(sellerA, true));
        addressRepository.saveAndFlush(address(sellerA, false));
        addressRepository.saveAndFlush(address(sellerB, false));

        List<SellerAddress> result = addressRepository.findBySellerId(sellerA.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(a -> a.getSeller().getId().equals(sellerA.getId()));
    }

    @Test
    void findByIdAndSellerId_existente_devuelveDireccion() {
        Seller seller = saveSeller("kc-001", "a@ekko.test");
        SellerAddress address = addressRepository.saveAndFlush(address(seller, false));

        Optional<SellerAddress> result = addressRepository.findByIdAndSellerId(address.getId(), seller.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(address.getId());
    }

    @Test
    void findByIdAndSellerId_deOtroVendedor_devuelveVacio() {
        Seller sellerA = saveSeller("kc-001", "a@ekko.test");
        Seller sellerB = saveSeller("kc-002", "b@ekko.test");
        SellerAddress address = addressRepository.saveAndFlush(address(sellerA, false));

        Optional<SellerAddress> result = addressRepository.findByIdAndSellerId(address.getId(), sellerB.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void existsBySellerId_devuelveVerdaderoYFalso() {
        Seller sellerA = saveSeller("kc-001", "a@ekko.test");
        Seller sellerB = saveSeller("kc-002", "b@ekko.test");
        addressRepository.saveAndFlush(address(sellerA, false));

        assertThat(addressRepository.existsBySellerId(sellerA.getId())).isTrue();
        assertThat(addressRepository.existsBySellerId(sellerB.getId())).isFalse();
    }

    @Test
    void countBySellerId_cuentaDirecciones() {
        Seller seller = saveSeller("kc-001", "a@ekko.test");
        addressRepository.saveAndFlush(address(seller, false));
        addressRepository.saveAndFlush(address(seller, false));

        assertThat(addressRepository.countBySellerId(seller.getId())).isEqualTo(2L);
    }

    @Test
    void clearPrimaryBySellerId_limpiaTodasLasPrimarias() {
        Seller seller = saveSeller("kc-001", "a@ekko.test");
        addressRepository.saveAndFlush(address(seller, true));
        addressRepository.saveAndFlush(address(seller, false));

        addressRepository.clearPrimaryBySellerId(seller.getId());

        assertThat(addressRepository.findBySellerId(seller.getId()))
                .allMatch(a -> !a.getPrimary());
    }

    @Test
    void constraintUkSellerAddressPrimary_soloUnaPrimariaPorVendedor() {
        Seller seller = saveSeller("kc-001", "a@ekko.test");
        addressRepository.saveAndFlush(address(seller, true));

        assertThatThrownBy(() -> addressRepository.saveAndFlush(address(seller, true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void constraintFkVendedorInexistente_lanzaViolacion() {
        Seller ghost = Seller.builder().id(UUID.randomUUID()).build();

        assertThatThrownBy(() -> addressRepository.saveAndFlush(address(ghost, false)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void indiceUnicoPrimariaExisteEnEsquema() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'uk_seller_address_primary'",
                Integer.class);

        assertThat(count).isEqualTo(1);
    }

    private Seller saveSeller(String keycloakId, String email) {
        return sellerRepository.saveAndFlush(Seller.builder()
                .keycloakId(keycloakId)
                .storeName("Mi tienda")
                .email(email)
                .status(com.ekko.seller_service.enums.SellerStatus.ACTIVE)
                .build());
    }

    private static SellerAddress address(Seller seller, boolean primary) {
        return SellerAddress.builder()
                .seller(seller)
                .addressLine("Calle 1 #2-3")
                .city("Sincelejo")
                .state("Sucre")
                .country("Colombia")
                .postalCode("700001")
                .primary(primary)
                .build();
    }
}
