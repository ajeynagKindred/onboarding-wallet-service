package org.example.walletservice.jms.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.walletservice.WalletRepository;
import org.example.walletservice.dto.CustomerUpdateEvent;
import org.example.walletservice.dto.UpdateEvent;
import org.example.walletservice.enums.ActionType;
import org.example.walletservice.jms.publisher.EventPublisher;
import org.example.walletservice.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Recover;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class UserUpdateEventListener {


    private WalletService walletService;


    private ObjectMapper objectMapper;


    private final EventPublisher eventPublisher;


    @Autowired
    public UserUpdateEventListener( EventPublisher eventPublisher,ObjectMapper objectMapper, WalletService walletService) {
        this.eventPublisher = eventPublisher;
        this.walletService = walletService;
        this.objectMapper = objectMapper;
    }

    @JmsListener(destination = "customer-update-queue")
    public void onMessage(String message) throws JsonProcessingException {
        log.info("Received message: {}", message);

        CustomerUpdateEvent customerUpdateEvent = objectMapper.readValue(message, CustomerUpdateEvent.class);
        try {
            createWalletWithRetry(customerUpdateEvent);
        } catch (Exception e) {
            log.error("Failed to process message after retries: {}", message, e);
            // Optionally send to a dead-letter queue or other recovery mechanism

        }
    }

    @Retryable(
            value = {Exception.class}, // Adjust this to the specific exception types expected
            maxAttempts = 5,
            backoff = @Backoff(delay = 2000) // 2-second delay between retries
    )
    private void createWalletWithRetry(CustomerUpdateEvent customerUpdateEvent) {
        try {
            walletService.createWallet(customerUpdateEvent.getCustomerId(), BigDecimal.ZERO);
        } catch (Exception e) {
            log.error("Error creating wallet for customerId: {}", customerUpdateEvent.getCustomerId(), e);
            throw e; // Rethrow to trigger retry mechanism
        }
    }

    @Recover
    public void recover(Exception e, CustomerUpdateEvent customerUpdateEvent) throws JsonProcessingException {
        log.error("Failed to create wallet after retries for customerId: {}", customerUpdateEvent.getCustomerId(), e);
        eventPublisher.publish(customerUpdateEvent, "dead-letter-queue-wallet-create");
    }



}
