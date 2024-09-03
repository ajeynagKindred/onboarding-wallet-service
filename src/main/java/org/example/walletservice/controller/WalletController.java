package org.example.walletservice.controller;


import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.walletservice.dto.DepositRequestBody;
import org.example.walletservice.dto.WithdrawRequestBody;
import org.example.walletservice.entity.Wallet;
import org.example.walletservice.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @PostMapping("/deposit")
    public ResponseEntity<Wallet> deposit(@RequestHeader Long userId,@RequestHeader String requestId, @RequestBody @Valid DepositRequestBody depositRequestBody) {
        log.info("Received request for deposit of {} for user : {} ",depositRequestBody.getAmount(),userId);
        return ResponseEntity.ok().body(walletService.deposit(userId, BigDecimal.valueOf(depositRequestBody.getAmount()),requestId));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Wallet> withdraw(@RequestHeader Long userId,@RequestHeader String requestId,  @RequestBody @Valid WithdrawRequestBody withdrawRequestBody) {
        log.info("Received request for withdrawal of {} for user : {} ",withdrawRequestBody.getAmount(),userId);
        return ResponseEntity.ok().body(walletService.withdraw(userId, BigDecimal.valueOf(withdrawRequestBody.getAmount()),requestId));
    }

    @GetMapping("/balance")
    public ResponseEntity<Wallet>  getWallet(@RequestHeader String userId) {
        log.info("Received request for getting wallet balance for user : {}  ",userId);
        return ResponseEntity.ok().body(walletService.getWallet(Long.valueOf(userId)));
    }
}