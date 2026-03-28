package com.sentbe.wallets.domain.wallet.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sentbe.wallets.common.enums.WalletTransactionStatus;
import com.sentbe.wallets.common.enums.WalletTransactionType;
import com.sentbe.wallets.domain.wallet.entity.WalletTransaction;

public record WalletTransactionListDto(
    Long id,
    String transactionId,
    WalletTransactionType type,
    WalletTransactionStatus status,
    Long amount,
    Long remainBalance,
    String errorCode,
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss")
    LocalDateTime requestedAt,
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss")
    LocalDateTime createdAt
) {
    public static WalletTransactionListDto from(WalletTransaction walletTransaction) {
        String errorCode = walletTransaction.getErrorCode() == null ? null : walletTransaction.getErrorCode().name();

        return new WalletTransactionListDto(
            walletTransaction.getId(),
            walletTransaction.getTransactionId(),
            walletTransaction.getType(),
            walletTransaction.getStatus(),
            walletTransaction.getAmount(),
            walletTransaction.getRemainBalance(),
            errorCode,
            walletTransaction.getRequestedAt(),
            walletTransaction.getCreatedAt()
        );
    }
}
