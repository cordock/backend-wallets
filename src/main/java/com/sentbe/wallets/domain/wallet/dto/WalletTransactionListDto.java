package com.sentbe.wallets.domain.wallet.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sentbe.wallets.common.enums.WalletTransactionStatus;
import com.sentbe.wallets.common.enums.WalletTransactionType;
import com.sentbe.wallets.domain.wallet.entity.WalletTransaction;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "WalletTransactionListDto", description = "월렛 입출금 내역 조회 모델")
public record WalletTransactionListDto(

    @Schema(description = "월렛 입출금 내역 ID", example = "1")
    Long id,

    @Schema(description = "월렛 입출금 거래 ID", example = "TX_20260101_00001")
    String transactionId,

    @Schema(description = "월렛 입출금 타입", example = "WITHDRAW")
    WalletTransactionType type,

    @Schema(description = "월렛 입출금 상태", example = "SUCCESS")
    WalletTransactionStatus status,

    @Schema(description = "월렛 입출금 금액", example = "10000")
    Long amount,

    @Schema(description = "잔여 월렛 금액", example = "95000")
    Long remainBalance,

    @Schema(description = "실패 사유 코드", example = "INSUFFICIENT_BALANCE")
    String errorCode,

    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss")
    @Schema(description = "입출금 요청일시", example = "2026-01-01 10:00:00")
    LocalDateTime requestedAt,

    @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss")
    @Schema(description = "생성일시", example = "2026-01-01 10:02:00")
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
