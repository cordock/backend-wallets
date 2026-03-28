package com.sentbe.wallets.domain.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WalletWithdrawalReqDto(
    @NotNull
    @Positive
    Long amount,

    @NotBlank
    String transactionId
) {
}
