package com.ekko.seller_service.service;

import com.ekko.seller_service.dto.request.SellerAddressRequest;
import com.ekko.seller_service.dto.response.SellerAddressResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerAddress;
import com.ekko.seller_service.exception.LastAddressDeleteException;
import com.ekko.seller_service.exception.PrimaryAddressDeleteException;
import com.ekko.seller_service.exception.SellerAddressNotFoundException;
import com.ekko.seller_service.exception.SellerAlreadyActiveException;
import com.ekko.seller_service.exception.SellerNotFoundException;
import com.ekko.seller_service.exception.SellerSuspendedException;
import com.ekko.seller_service.exception.SellerPendingException;
import com.ekko.seller_service.mapper.SellerMapper;
import com.ekko.seller_service.repository.SellerAddressRepository;
import com.ekko.seller_service.repository.SellerRepository;
import com.ekko.seller_service.support.PrimaryEntityPolicy;
import com.ekko.seller_service.support.SellerOperationValidator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.ekko.seller_service.support.AddressTestDataBuilder.anAddress;
import static com.ekko.seller_service.support.SellerTestDataBuilder.aSeller;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerAddressServiceTest {

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID ADDRESS_ID = UUID.randomUUID();

    private static final SellerAddressRequest REQUEST = new SellerAddressRequest(
            "Calle 10 #20-30", "Sincelejo", "Sucre", "Colombia", "700001");

    @Mock
    private SellerAddressRepository addressRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Spy
    private PrimaryEntityPolicy primaryPolicy;

    @Spy
    private SellerOperationValidator validator;

    @Mock
    private SellerMapper sellerMapper;

    @InjectMocks
    private SellerAddressService sellerAddressService;

    @Nested
    class AddAddress {

        @Test
        void primeraDireccion_seCreaComoPrimaria() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            when(sellerRepository.findByIdForUpdate(SELLER_ID)).thenReturn(Optional.of(seller));
            when(addressRepository.existsBySellerId(SELLER_ID)).thenReturn(false);
            when(addressRepository.save(any(SellerAddress.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sellerMapper.toAddressResponse(any(SellerAddress.class)))
                    .thenAnswer(inv -> sellerAddressResponseFor(inv.getArgument(0)));

            SellerAddressResponse result = sellerAddressService.addAddress(SELLER_ID, REQUEST);

            assertTrue(result.isPrimary());
            ArgumentCaptor<SellerAddress> captor = ArgumentCaptor.forClass(SellerAddress.class);
            verify(addressRepository).save(captor.capture());
            SellerAddress created = captor.getValue();
            assertTrue(created.getPrimary());
            assertEquals(seller, created.getSeller());
            assertEquals("Calle 10 #20-30", created.getAddressLine());
            assertEquals("Sincelejo", created.getCity());
            assertEquals("Sucre", created.getState());
            assertEquals("Colombia", created.getCountry());
            assertEquals("700001", created.getPostalCode());
        }

        @Test
        void direccionAdicional_seCreaComoNoPrimaria() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            when(sellerRepository.findByIdForUpdate(SELLER_ID)).thenReturn(Optional.of(seller));
            when(addressRepository.existsBySellerId(SELLER_ID)).thenReturn(true);
            when(addressRepository.save(any(SellerAddress.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sellerMapper.toAddressResponse(any(SellerAddress.class)))
                    .thenAnswer(inv -> sellerAddressResponseFor(inv.getArgument(0)));

            SellerAddressResponse result = sellerAddressService.addAddress(SELLER_ID, REQUEST);

            assertFalse(result.isPrimary());
            ArgumentCaptor<SellerAddress> captor = ArgumentCaptor.forClass(SellerAddress.class);
            verify(addressRepository).save(captor.capture());
            assertFalse(captor.getValue().getPrimary());
        }

        @Test
        void vendedorInexistente_lanzaSellerNotFound() {
            when(sellerRepository.findByIdForUpdate(SELLER_ID)).thenReturn(Optional.empty());

            assertThrows(SellerNotFoundException.class,
                    () -> sellerAddressService.addAddress(SELLER_ID, REQUEST));

            verify(addressRepository, never()).save(any(SellerAddress.class));
        }

        @Test
        void vendedorSuspendido_lanzaSellerSuspended() {
            Seller seller = aSeller().withId(SELLER_ID).suspended().build();
            when(sellerRepository.findByIdForUpdate(SELLER_ID)).thenReturn(Optional.of(seller));

            assertThrows(SellerSuspendedException.class,
                    () -> sellerAddressService.addAddress(SELLER_ID, REQUEST));

            verify(addressRepository, never()).save(any(SellerAddress.class));
        }
    }

    @Nested
    class UpdateAddress {

        @Test
        void actualizacionCorrecta_actualizaCampos() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            SellerAddress address = anAddress().withId(ADDRESS_ID).withSeller(seller).build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(addressRepository.findByIdAndSellerId(ADDRESS_ID, SELLER_ID)).thenReturn(Optional.of(address));
            when(addressRepository.save(address)).thenReturn(address);
            when(sellerMapper.toAddressResponse(address))
                    .thenAnswer(inv -> sellerAddressResponseFor(address));

            SellerAddressResponse result = sellerAddressService.updateAddress(
                    SELLER_ID, ADDRESS_ID, REQUEST);

            assertEquals("Calle 10 #20-30", address.getAddressLine());
            assertEquals("Sincelejo", address.getCity());
            assertEquals("Sucre", address.getState());
            assertEquals("Colombia", address.getCountry());
            assertEquals("700001", address.getPostalCode());
            verify(addressRepository).save(address);
            assertEquals(sellerAddressResponseFor(address), result);
        }

        @Test
        void vendedorActivo_lanzaSellerAlreadyActive() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));

            assertThrows(SellerAlreadyActiveException.class,
                    () -> sellerAddressService.updateAddress(SELLER_ID, ADDRESS_ID, REQUEST));

            verify(addressRepository, never()).save(any(SellerAddress.class));
        }

        @Test
        void direccionInexistente_lanzaAddressNotFound() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(addressRepository.findByIdAndSellerId(ADDRESS_ID, SELLER_ID)).thenReturn(Optional.empty());

            assertThrows(SellerAddressNotFoundException.class,
                    () -> sellerAddressService.updateAddress(SELLER_ID, ADDRESS_ID, REQUEST));

            verify(addressRepository, never()).save(any(SellerAddress.class));
        }

        @Test
        void direccionDeOtroVendedor_lanzaAddressNotFound() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(addressRepository.findByIdAndSellerId(ADDRESS_ID, SELLER_ID)).thenReturn(Optional.empty());

            assertThrows(SellerAddressNotFoundException.class,
                    () -> sellerAddressService.updateAddress(SELLER_ID, ADDRESS_ID, REQUEST));

            verify(addressRepository, never()).save(any(SellerAddress.class));
        }
    }

    @Nested
    class DeleteAddress {

        @Test
        void eliminacionCorrecta_eliminaDireccion() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            SellerAddress address = anAddress().withId(ADDRESS_ID).withSeller(seller).notPrimary().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(addressRepository.findByIdAndSellerId(ADDRESS_ID, SELLER_ID)).thenReturn(Optional.of(address));
            when(addressRepository.countBySellerId(SELLER_ID)).thenReturn(2L);

            sellerAddressService.deleteAddress(SELLER_ID, ADDRESS_ID);

            verify(addressRepository).delete(address);
        }

        @Test
        void vendedorActivo_lanzaSellerAlreadyActive() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));

            assertThrows(SellerAlreadyActiveException.class,
                    () -> sellerAddressService.deleteAddress(SELLER_ID, ADDRESS_ID));

            verify(addressRepository, never()).delete(any(SellerAddress.class));
        }

        @Test
        void ultimaDireccion_lanzaLastAddressDelete() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            SellerAddress address = anAddress().withId(ADDRESS_ID).withSeller(seller).notPrimary().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(addressRepository.findByIdAndSellerId(ADDRESS_ID, SELLER_ID)).thenReturn(Optional.of(address));
            when(addressRepository.countBySellerId(SELLER_ID)).thenReturn(1L);

            assertThrows(LastAddressDeleteException.class,
                    () -> sellerAddressService.deleteAddress(SELLER_ID, ADDRESS_ID));

            verify(addressRepository, never()).delete(any(SellerAddress.class));
        }

        @Test
        void direccionPrimaria_lanzaPrimaryAddressDelete() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            SellerAddress address = anAddress().withId(ADDRESS_ID).withSeller(seller).primary().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(addressRepository.findByIdAndSellerId(ADDRESS_ID, SELLER_ID)).thenReturn(Optional.of(address));
            when(addressRepository.countBySellerId(SELLER_ID)).thenReturn(2L);

            assertThrows(PrimaryAddressDeleteException.class,
                    () -> sellerAddressService.deleteAddress(SELLER_ID, ADDRESS_ID));

            verify(addressRepository, never()).delete(any(SellerAddress.class));
        }
    }

    @Nested
    class SetPrimaryAddress {

        @Test
        void promueveDireccion_limpiaPrimariasYGuarda() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            SellerAddress address = anAddress().withId(ADDRESS_ID).withSeller(seller).notPrimary().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(addressRepository.findByIdAndSellerId(ADDRESS_ID, SELLER_ID)).thenReturn(Optional.of(address));
            when(addressRepository.save(address)).thenReturn(address);
            when(sellerMapper.toAddressResponse(address))
                    .thenAnswer(inv -> sellerAddressResponseFor(address));

            SellerAddressResponse result = sellerAddressService.setPrimaryAddress(SELLER_ID, ADDRESS_ID);

            assertTrue(result.isPrimary());
            assertTrue(address.getPrimary());
            verify(addressRepository).clearPrimaryBySellerId(SELLER_ID);
            verify(addressRepository).save(address);
        }

        @Test
        void vendedorActivo_lanzaSellerAlreadyActive() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));

            assertThrows(SellerAlreadyActiveException.class,
                    () -> sellerAddressService.setPrimaryAddress(SELLER_ID, ADDRESS_ID));

            verify(addressRepository, never()).clearPrimaryBySellerId(any(UUID.class));
            verify(addressRepository, never()).save(any(SellerAddress.class));
        }

        @Test
        void yaEraPrimaria_noHaceNada() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            SellerAddress address = anAddress().withId(ADDRESS_ID).withSeller(seller).primary().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(addressRepository.findByIdAndSellerId(ADDRESS_ID, SELLER_ID)).thenReturn(Optional.of(address));
            when(sellerMapper.toAddressResponse(address)).thenReturn(sellerAddressResponseFor(address));

            SellerAddressResponse result = sellerAddressService.setPrimaryAddress(SELLER_ID, ADDRESS_ID);

            assertTrue(result.isPrimary());
            verify(addressRepository, never()).clearPrimaryBySellerId(any(UUID.class));
            verify(addressRepository, never()).save(any(SellerAddress.class));
        }

        @Test
        void direccionInexistente_lanzaAddressNotFound() {
            Seller seller = aSeller().withId(SELLER_ID).pendingReview().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(addressRepository.findByIdAndSellerId(ADDRESS_ID, SELLER_ID)).thenReturn(Optional.empty());

            assertThrows(SellerAddressNotFoundException.class,
                    () -> sellerAddressService.setPrimaryAddress(SELLER_ID, ADDRESS_ID));

            verify(addressRepository, never()).clearPrimaryBySellerId(any(UUID.class));
        }
    }

    private static SellerAddressResponse sellerAddressResponseFor(SellerAddress address) {
        return new SellerAddressResponse(
                address.getId(),
                address.getAddressLine(),
                address.getCity(),
                address.getState(),
                address.getCountry(),
                address.getPostalCode(),
                address.getPrimary(),
                address.getCreatedAt());
    }
}