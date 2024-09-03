package org.example.walletservice.service;

import lombok.extern.slf4j.Slf4j;
import org.example.walletservice.WalletRepository;
import org.example.walletservice.dto.UpdateEvent;
import org.example.walletservice.entity.Wallet;
import org.example.walletservice.enums.ActionType;
import org.example.walletservice.exception.InsufficientBalanceException;
import org.example.walletservice.exception.InvalidCustomerException;
import org.example.walletservice.jms.publisher.EventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final EventPublisher eventPublisher;
    public final Set<String> processedRequestIds = new HashSet<>();

    @Autowired
    public WalletService(WalletRepository walletRepository, EventPublisher eventPublisher) {
        this.walletRepository = walletRepository;
        this.eventPublisher = eventPublisher;
    }


    public void createWallet(Long customerId, BigDecimal initialBalance) {


        log.info("Creating wallet for customerId :{} ", customerId);
        Optional<Wallet> existingWallet = walletRepository.findByCustomerId(customerId);
        if (existingWallet.isPresent()) {
            log.info("Wallet already exists for customerId :{} ", customerId);
            return; // No need to create a new wallet if it already exists
        }

        Wallet wallet = Wallet.builder()
                .customerId(customerId)
                .balance(initialBalance)
                .build();
        walletRepository.save(wallet);
        log.info("Successfully created wallet for customerId :{} ", customerId);

    }

    public Wallet deposit(Long id, BigDecimal amount, String requestId) {
        if (processedRequestIds.contains(requestId)) {
            log.info("Request ID {} already processed. Skipping deposit.", requestId);
            return walletRepository.findByCustomerId(id)
                    .orElseThrow(() -> new InvalidCustomerException("Customer with id " + id + " does not exist"));
        }

        log.info("Depositing {} for customerId :{} ", amount, id);
        Optional<Wallet> wallet = walletRepository.findByCustomerId(id);
        if (wallet.isEmpty()) {
            log.info("Customer Not Present: {}", id);
            throw new InvalidCustomerException("Customer with id " + id + " does not exist");
        }
        Wallet existingWallet = wallet.get();
        BigDecimal newBalance = existingWallet.getBalance().add(amount);
        existingWallet.setBalance(newBalance);
        Wallet savedWallet = walletRepository.save(existingWallet);
        log.info("Deposited {} for customerId :{}. New Balance: {}", amount, id, newBalance);
        eventPublisher.publish(new UpdateEvent(id, amount, ActionType.DEBIT, newBalance),"balance-update-event");
        processedRequestIds.add(requestId);
        return savedWallet;
    }

    public Wallet withdraw(Long id, BigDecimal amount, String requestId) {
        if (processedRequestIds.contains(requestId)) {
            log.info("Request ID {} already processed. Skipping withdrawal.", requestId);
            return walletRepository.findByCustomerId(id)
                    .orElseThrow(() -> new InvalidCustomerException("Customer with id " + id + " does not exist"));
        }

        log.info("Withdrawing {} for customerId :{} ", amount, id);
        Optional<Wallet> wallet = walletRepository.findByCustomerId(id);
        if (wallet.isEmpty()) {
            log.info("Customer Not Present: {}", id);
            throw new InvalidCustomerException("Customer with id " + id + " does not exist");
        }
        Wallet existingWallet = wallet.get();
        if (existingWallet.getBalance().compareTo(amount) < 0) {
            log.info("Insufficient balance to withdraw. Balance: {}, Withdrawal Request: {}, UserId: {}",
                    existingWallet.getBalance(), amount, id);
            throw new InsufficientBalanceException("Insufficient funds");
        }
        BigDecimal newBalance = existingWallet.getBalance().subtract(amount);
        existingWallet.setBalance(newBalance);
        Wallet savedWallet = walletRepository.save(existingWallet);
        log.info("Withdrew {} for customerId :{} . New Balance: {}", amount, id, newBalance);
        eventPublisher.publish(new UpdateEvent(id, amount, ActionType.CREDIT, newBalance),"balance-update-event");
        processedRequestIds.add(requestId);
        return savedWallet;
    }

    public Wallet getWallet(Long id) {
        log.info("Balance check for customerId :{} ", id);
        return walletRepository.findByCustomerId(id)
                .orElseThrow(() -> new InvalidCustomerException("Wallet not found for user with id " + id));
    }
}
