package com.ekko.seller_service.service;

import com.ekko.seller_service.dto.request.SellerBankAccountRequest;
import com.ekko.seller_service.dto.response.SellerBankAccountResponse;
import com.ekko.seller_service.entity.Seller;
import com.ekko.seller_service.entity.SellerBankAccount;
import com.ekko.seller_service.enums.BankAccountType;
import com.ekko.seller_service.exception.DuplicateSellerBankAccountException;
import com.ekko.seller_service.exception.LastBankAccountDeleteException;
import com.ekko.seller_service.exception.PrimaryBankAccountDeleteException;
import com.ekko.seller_service.exception.SellerBankAccountNotFoundException;
import com.ekko.seller_service.exception.SellerNotFoundException;
import com.ekko.seller_service.exception.SellerSuspendedException;
import com.ekko.seller_service.mapper.SellerMapper;
import com.ekko.seller_service.repository.SellerBankAccountRepository;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static com.ekko.seller_service.support.BankAccountTestDataBuilder.aBankAccount;
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
class SellerBankAccountServiceTest {

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    private static final SellerBankAccountRequest REQUEST = new SellerBankAccountRequest(
            "Bancolombia", BankAccountType.SAVINGS, "1234567890", "Vendedor Ejemplo");

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerBankAccountRepository bankRepository;

    @Spy
    private SellerOperationValidator validator;

    @Spy
    private PrimaryEntityPolicy primaryPolicy;

    @Mock
    private SellerMapper sellerMapper;

    @InjectMocks
    private SellerBankAccountService sellerBankAccountService;

    @Nested
    class AddBankAccount {

        @Test
        void primeraCuenta_seCreaComoPrimaria() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            when(sellerRepository.findByIdForUpdate(SELLER_ID)).thenReturn(Optional.of(seller));
            when(bankRepository.existsBySellerIdAndBankNameAndAccountNumber(
                    SELLER_ID, REQUEST.bankName(), REQUEST.accountNumber())).thenReturn(false);
            when(bankRepository.existsBySellerId(SELLER_ID)).thenReturn(false);
            when(bankRepository.save(any(SellerBankAccount.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sellerMapper.toBankAccountResponse(any(SellerBankAccount.class)))
                    .thenAnswer(inv -> bankAccountResponseFor(inv.getArgument(0)));

            SellerBankAccountResponse result = sellerBankAccountService.addBankAccount(SELLER_ID, REQUEST);

            assertTrue(result.isPrimary());
            ArgumentCaptor<SellerBankAccount> captor = ArgumentCaptor.forClass(SellerBankAccount.class);
            verify(bankRepository).save(captor.capture());
            SellerBankAccount created = captor.getValue();
            assertTrue(created.getIsPrimary());
            assertEquals(seller, created.getSeller());
            assertEquals("Bancolombia", created.getBankName());
            assertEquals(BankAccountType.SAVINGS, created.getAccountType());
            assertEquals("1234567890", created.getAccountNumber());
            assertEquals("Vendedor Ejemplo", created.getAccountHolder());
        }

        @Test
        void cuentaAdicional_seCreaComoNoPrimaria() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            when(sellerRepository.findByIdForUpdate(SELLER_ID)).thenReturn(Optional.of(seller));
            when(bankRepository.existsBySellerIdAndBankNameAndAccountNumber(
                    SELLER_ID, REQUEST.bankName(), REQUEST.accountNumber())).thenReturn(false);
            when(bankRepository.existsBySellerId(SELLER_ID)).thenReturn(true);
            when(bankRepository.save(any(SellerBankAccount.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sellerMapper.toBankAccountResponse(any(SellerBankAccount.class)))
                    .thenAnswer(inv -> bankAccountResponseFor(inv.getArgument(0)));

            SellerBankAccountResponse result = sellerBankAccountService.addBankAccount(SELLER_ID, REQUEST);

            assertFalse(result.isPrimary());
            ArgumentCaptor<SellerBankAccount> captor = ArgumentCaptor.forClass(SellerBankAccount.class);
            verify(bankRepository).save(captor.capture());
            assertFalse(captor.getValue().getIsPrimary());
        }

        @Test
        void duplicadoPrevisto_lanzaDuplicateBankAccount() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            when(sellerRepository.findByIdForUpdate(SELLER_ID)).thenReturn(Optional.of(seller));
            when(bankRepository.existsBySellerIdAndBankNameAndAccountNumber(
                    SELLER_ID, REQUEST.bankName(), REQUEST.accountNumber())).thenReturn(true);

            assertThrows(DuplicateSellerBankAccountException.class,
                    () -> sellerBankAccountService.addBankAccount(SELLER_ID, REQUEST));

            verify(bankRepository, never()).save(any(SellerBankAccount.class));
        }

        @Test
        void dataIntegrityViolation_conUniqueConstraint_lanzaDuplicateBankAccount() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            SQLException sql = new SQLException(
                    "duplicate key value violates unique constraint \"uk_seller_bank_account\"", "23505");
            DataIntegrityViolationException violation = new DataIntegrityViolationException("stmt", sql);
            when(sellerRepository.findByIdForUpdate(SELLER_ID)).thenReturn(Optional.of(seller));
            when(bankRepository.existsBySellerIdAndBankNameAndAccountNumber(
                    SELLER_ID, REQUEST.bankName(), REQUEST.accountNumber())).thenReturn(false);
            when(bankRepository.existsBySellerId(SELLER_ID)).thenReturn(false);
            when(bankRepository.save(any(SellerBankAccount.class))).thenThrow(violation);

            assertThrows(DuplicateSellerBankAccountException.class,
                    () -> sellerBankAccountService.addBankAccount(SELLER_ID, REQUEST));
        }

        @Test
        void vendedorInexistente_lanzaSellerNotFound() {
            when(sellerRepository.findByIdForUpdate(SELLER_ID)).thenReturn(Optional.empty());

            assertThrows(SellerNotFoundException.class,
                    () -> sellerBankAccountService.addBankAccount(SELLER_ID, REQUEST));

            verify(bankRepository, never()).save(any(SellerBankAccount.class));
        }

        @Test
        void vendedorSuspendido_lanzaSellerSuspended() {
            Seller seller = aSeller().withId(SELLER_ID).suspended().build();
            when(sellerRepository.findByIdForUpdate(SELLER_ID)).thenReturn(Optional.of(seller));

            assertThrows(SellerSuspendedException.class,
                    () -> sellerBankAccountService.addBankAccount(SELLER_ID, REQUEST));

            verify(bankRepository, never()).save(any(SellerBankAccount.class));
        }
    }

    @Nested
    class UpdateBankAccount {

        @Test
        void actualizacionCorrecta_actualizaCampos() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            SellerBankAccount account = aBankAccount().withId(ACCOUNT_ID).withSeller(seller).notPrimary().build();
            SellerBankAccountRequest request = new SellerBankAccountRequest(
                    "BBVA", BankAccountType.CHECKING, "0987654321", "Nuevo Titular");
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(bankRepository.findByIdAndSellerId(ACCOUNT_ID, SELLER_ID)).thenReturn(Optional.of(account));
            when(bankRepository.existsBySellerIdAndBankNameAndAccountNumberAndIdNot(
                    SELLER_ID, request.bankName(), request.accountNumber(), ACCOUNT_ID)).thenReturn(false);
            when(bankRepository.save(account)).thenReturn(account);
            when(sellerMapper.toBankAccountResponse(account))
                    .thenAnswer(inv -> bankAccountResponseFor(account));

            SellerBankAccountResponse result = sellerBankAccountService.updateBankAccount(
                    SELLER_ID, ACCOUNT_ID, request);

            assertEquals("BBVA", account.getBankName());
            assertEquals(BankAccountType.CHECKING, account.getAccountType());
            assertEquals("0987654321", account.getAccountNumber());
            assertEquals("Nuevo Titular", account.getAccountHolder());
            verify(bankRepository).save(account);
            assertEquals(bankAccountResponseFor(account), result);
        }

        @Test
        void cuentaInexistente_lanzaBankAccountNotFound() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(bankRepository.findByIdAndSellerId(ACCOUNT_ID, SELLER_ID)).thenReturn(Optional.empty());

            assertThrows(SellerBankAccountNotFoundException.class,
                    () -> sellerBankAccountService.updateBankAccount(SELLER_ID, ACCOUNT_ID, REQUEST));

            verify(bankRepository, never()).save(any(SellerBankAccount.class));
        }

        @Test
        void duplicadoEnActualizacion_lanzaDuplicateBankAccount() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            SellerBankAccount account = aBankAccount().withId(ACCOUNT_ID).withSeller(seller).build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(bankRepository.findByIdAndSellerId(ACCOUNT_ID, SELLER_ID)).thenReturn(Optional.of(account));
            when(bankRepository.existsBySellerIdAndBankNameAndAccountNumberAndIdNot(
                    SELLER_ID, REQUEST.bankName(), REQUEST.accountNumber(), ACCOUNT_ID)).thenReturn(true);

            assertThrows(DuplicateSellerBankAccountException.class,
                    () -> sellerBankAccountService.updateBankAccount(SELLER_ID, ACCOUNT_ID, REQUEST));

            verify(bankRepository, never()).save(any(SellerBankAccount.class));
        }
    }

    @Nested
    class DeleteBankAccount {

        @Test
        void eliminacionCorrecta_eliminaCuenta() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            SellerBankAccount account = aBankAccount().withId(ACCOUNT_ID).withSeller(seller).notPrimary().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(bankRepository.findByIdAndSellerId(ACCOUNT_ID, SELLER_ID)).thenReturn(Optional.of(account));
            when(bankRepository.countBySellerId(SELLER_ID)).thenReturn(2L);

            sellerBankAccountService.deleteBankAccount(SELLER_ID, ACCOUNT_ID);

            verify(bankRepository).delete(account);
        }

        @Test
        void ultimaCuenta_lanzaLastBankAccountDelete() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            SellerBankAccount account = aBankAccount().withId(ACCOUNT_ID).withSeller(seller).notPrimary().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(bankRepository.findByIdAndSellerId(ACCOUNT_ID, SELLER_ID)).thenReturn(Optional.of(account));
            when(bankRepository.countBySellerId(SELLER_ID)).thenReturn(1L);

            assertThrows(LastBankAccountDeleteException.class,
                    () -> sellerBankAccountService.deleteBankAccount(SELLER_ID, ACCOUNT_ID));

            verify(bankRepository, never()).delete(any(SellerBankAccount.class));
        }

        @Test
        void cuentaPrimaria_lanzaPrimaryBankAccountDelete() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            SellerBankAccount account = aBankAccount().withId(ACCOUNT_ID).withSeller(seller).primary().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(bankRepository.findByIdAndSellerId(ACCOUNT_ID, SELLER_ID)).thenReturn(Optional.of(account));
            when(bankRepository.countBySellerId(SELLER_ID)).thenReturn(2L);

            assertThrows(PrimaryBankAccountDeleteException.class,
                    () -> sellerBankAccountService.deleteBankAccount(SELLER_ID, ACCOUNT_ID));

            verify(bankRepository, never()).delete(any(SellerBankAccount.class));
        }
    }

    @Nested
    class SetPrimaryBankAccount {

        @Test
        void promueveCuenta_limpiaPrimariasYGuarda() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            SellerBankAccount account = aBankAccount().withId(ACCOUNT_ID).withSeller(seller).notPrimary().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(bankRepository.findByIdAndSellerId(ACCOUNT_ID, SELLER_ID)).thenReturn(Optional.of(account));
            when(bankRepository.save(account)).thenReturn(account);
            when(sellerMapper.toBankAccountResponse(account))
                    .thenAnswer(inv -> bankAccountResponseFor(account));

            SellerBankAccountResponse result = sellerBankAccountService.setPrimaryBankAccount(SELLER_ID, ACCOUNT_ID);

            assertTrue(result.isPrimary());
            assertTrue(account.getIsPrimary());
            verify(bankRepository).clearPrimaryBySellerId(SELLER_ID);
            verify(bankRepository).save(account);
        }

        @Test
        void yaEraPrimaria_noHaceNada() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            SellerBankAccount account = aBankAccount().withId(ACCOUNT_ID).withSeller(seller).primary().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(bankRepository.findByIdAndSellerId(ACCOUNT_ID, SELLER_ID)).thenReturn(Optional.of(account));
            when(sellerMapper.toBankAccountResponse(account))
                    .thenAnswer(inv -> bankAccountResponseFor(account));

            SellerBankAccountResponse result = sellerBankAccountService.setPrimaryBankAccount(SELLER_ID, ACCOUNT_ID);

            assertTrue(result.isPrimary());
            verify(bankRepository, never()).clearPrimaryBySellerId(any(UUID.class));
            verify(bankRepository, never()).save(any(SellerBankAccount.class));
        }

        @Test
        void cuentaInexistente_lanzaBankAccountNotFound() {
            Seller seller = aSeller().withId(SELLER_ID).active().build();
            when(sellerRepository.findById(SELLER_ID)).thenReturn(Optional.of(seller));
            when(bankRepository.findByIdAndSellerId(ACCOUNT_ID, SELLER_ID)).thenReturn(Optional.empty());

            assertThrows(SellerBankAccountNotFoundException.class,
                    () -> sellerBankAccountService.setPrimaryBankAccount(SELLER_ID, ACCOUNT_ID));

            verify(bankRepository, never()).clearPrimaryBySellerId(any(UUID.class));
        }
    }

    private static SellerBankAccountResponse bankAccountResponseFor(SellerBankAccount account) {
        return new SellerBankAccountResponse(
                account.getId(),
                account.getBankName(),
                account.getAccountType(),
                account.getAccountNumber(),
                account.getAccountHolder(),
                account.getIsPrimary(),
                account.getCreatedAt());
    }
}
