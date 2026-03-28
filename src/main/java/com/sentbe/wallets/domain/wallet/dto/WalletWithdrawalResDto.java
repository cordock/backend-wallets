package com.sentbe.wallets.domain.wallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sentbe.wallets.common.enums.WalletTransactionStatus;

public record WalletWithdrawalResDto(
    Long walletId,
    String transactionId,
    WalletTransactionStatus status,
    Long amount,
    Long remainBalance,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    String errorCode
) {
}
