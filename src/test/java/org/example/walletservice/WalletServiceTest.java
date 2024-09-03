package org.example.walletservice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.Optional;
import org.example.walletservice.dto.UpdateEvent;
import org.example.walletservice.entity.Wallet;
import org.example.walletservice.exception.InsufficientBalanceException;
import org.example.walletservice.exception.InvalidCustomerException;
import org.example.walletservice.jms.publisher.EventPublisher;
import org.example.walletservice.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateWalletWhenWalletDoesNotExist() {
        Long customerId = 1L;
        BigDecimal initialBalance = BigDecimal.valueOf(100);

        when(walletRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

        walletService.createWallet(customerId, initialBalance);

        verify(walletRepository).findByCustomerId(customerId);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void testCreateWalletWhenWalletExists() {
        Long customerId = 1L;
        BigDecimal initialBalance = BigDecimal.valueOf(100);
        Wallet existingWallet = Wallet.builder().customerId(customerId).balance(initialBalance).build();

        when(walletRepository.findByCustomerId(customerId)).thenReturn(Optional.of(existingWallet));

        walletService.createWallet(customerId, initialBalance);

        verify(walletRepository).findByCustomerId(customerId);
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void testDepositWhenWalletExistsAndRequestNotProcessed() {
        Long customerId = 1L;
        BigDecimal depositAmount = BigDecimal.valueOf(100);
        String requestId = "req123";
        Wallet existingWallet = Wallet.builder().customerId(customerId).balance(BigDecimal.valueOf(50)).build();
        Wallet updatedWallet = Wallet.builder().customerId(customerId).balance(BigDecimal.valueOf(150)).build();

        when(walletRepository.findByCustomerId(customerId)).thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(updatedWallet);

        walletService.deposit(customerId, depositAmount, requestId);

        verify(walletRepository).findByCustomerId(customerId);
        verify(walletRepository).save(any(Wallet.class));
        verify(eventPublisher).publish(any(UpdateEvent.class), anyString());
    }

    @Test
    void testDepositWhenWalletDoesNotExist() {
        Long customerId = 1L;
        BigDecimal depositAmount = BigDecimal.valueOf(100);
        String requestId = "req123";

        when(walletRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

        assertThrows(InvalidCustomerException.class, () -> {
            walletService.deposit(customerId, depositAmount, requestId);
        });

        verify(walletRepository).findByCustomerId(customerId);
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(eventPublisher, never()).publish(any(UpdateEvent.class), anyString());
    }

    @Test
    void testDepositWhenRequestAlreadyProcessed() {
        Long customerId = 1L;
        BigDecimal depositAmount = BigDecimal.valueOf(100);
        String requestId = "req123";
        Wallet existingWallet = Wallet.builder().customerId(customerId).balance(BigDecimal.valueOf(50)).build();

        walletService.processedRequestIds.add(requestId);
        when(walletRepository.findByCustomerId(customerId)).thenReturn(Optional.of(existingWallet));

        Wallet result = walletService.deposit(customerId, depositAmount, requestId);

        assertEquals(existingWallet, result);
        verify(walletRepository).findByCustomerId(customerId);
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(eventPublisher, never()).publish(any(UpdateEvent.class), anyString());
    }

    @Test
    void testWithdrawWhenWalletExistsAndRequestNotProcessed() {
        Long customerId = 1L;
        BigDecimal withdrawAmount = BigDecimal.valueOf(50);
        String requestId = "req123";
        Wallet existingWallet = Wallet.builder().customerId(customerId).balance(BigDecimal.valueOf(100)).build();
        Wallet updatedWallet = Wallet.builder().customerId(customerId).balance(BigDecimal.valueOf(50)).build();

        when(walletRepository.findByCustomerId(customerId)).thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(updatedWallet);

        walletService.withdraw(customerId, withdrawAmount, requestId);

        verify(walletRepository).findByCustomerId(customerId);
        verify(walletRepository).save(any(Wallet.class));
        verify(eventPublisher).publish(any(UpdateEvent.class), anyString());
    }

    @Test
    void testWithdrawWhenWalletDoesNotExist() {
        Long customerId = 1L;
        BigDecimal withdrawAmount = BigDecimal.valueOf(50);
        String requestId = "req123";

        when(walletRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

        assertThrows(InvalidCustomerException.class, () -> {
            walletService.withdraw(customerId, withdrawAmount, requestId);
        });

        verify(walletRepository).findByCustomerId(customerId);
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(eventPublisher, never()).publish(any(UpdateEvent.class), anyString());
    }

    @Test
    void testWithdrawWhenInsufficientBalance() {
        Long customerId = 1L;
        BigDecimal withdrawAmount = BigDecimal.valueOf(150);
        String requestId = "req123";
        Wallet existingWallet = Wallet.builder().customerId(customerId).balance(BigDecimal.valueOf(100)).build();

        when(walletRepository.findByCustomerId(customerId)).thenReturn(Optional.of(existingWallet));

        assertThrows(InsufficientBalanceException.class, () -> {
            walletService.withdraw(customerId, withdrawAmount, requestId);
        });

        verify(walletRepository).findByCustomerId(customerId);
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(eventPublisher, never()).publish(any(UpdateEvent.class), anyString());
    }

    @Test
    void testWithdrawWhenRequestAlreadyProcessed() {
        Long customerId = 1L;
        BigDecimal withdrawAmount = BigDecimal.valueOf(50);
        String requestId = "req123";
        Wallet existingWallet = Wallet.builder().customerId(customerId).balance(BigDecimal.valueOf(100)).build();

        walletService.processedRequestIds.add(requestId);
        when(walletRepository.findByCustomerId(customerId)).thenReturn(Optional.of(existingWallet));

        Wallet result = walletService.withdraw(customerId, withdrawAmount, requestId);

        assertEquals(existingWallet, result);
        verify(walletRepository).findByCustomerId(customerId);
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(eventPublisher, never()).publish(any(UpdateEvent.class), anyString());
    }

    @Test
    void testGetWallet() {
        Long customerId = 1L;
        Wallet wallet = Wallet.builder().customerId(customerId).balance(BigDecimal.valueOf(100)).build();

        when(walletRepository.findByCustomerId(customerId)).thenReturn(Optional.of(wallet));

        Wallet result = walletService.getWallet(customerId);

        assertNotNull(result);
        assertEquals(wallet, result);
        verify(walletRepository).findByCustomerId(customerId);
    }

    @Test
    void testGetWalletNotFound() {
        Long customerId = 1L;

        when(walletRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

        assertThrows(InvalidCustomerException.class, () -> {
            walletService.getWallet(customerId);
        });

        verify(walletRepository).findByCustomerId(customerId);
    }
}
