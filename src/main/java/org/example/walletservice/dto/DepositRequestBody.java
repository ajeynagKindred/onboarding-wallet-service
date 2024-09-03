package org.example.walletservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequestBody {

    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be a positive value")
    private Double amount;
}
