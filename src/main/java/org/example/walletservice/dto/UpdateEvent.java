package org.example.walletservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.walletservice.enums.ActionType;

import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEvent {

    private Long customerId;
    private BigDecimal amount;
    private ActionType actionType;
    private BigDecimal balance;

}

