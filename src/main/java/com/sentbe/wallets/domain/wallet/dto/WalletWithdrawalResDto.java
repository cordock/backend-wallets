package com.sentbe.wallets.domain.wallet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sentbe.wallets.common.enums.WalletTransactionStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "WalletWithdrawalResDto", description = "월렛 출금 응답 모델")
public record WalletWithdrawalResDto(

    @Schema(description = "월렛 ID", example = "1")
    Long walletId,

    @Schema(description = "월렛 입출금 거래 ID", example = "TX_20260101_00001")
    String transactionId,

    @Schema(description = "월렛 입출금 거래 상태", example = "SUCCESS")
    WalletTransactionStatus status,

    @Schema(description = "월렛 입출금 금액", example = "10000")
    Long amount,

    @Schema(description = "잔여 월렛 금액", example = "95000")
    Long remainBalance,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "실패 사유 코드", example = "INSUFFICIENT_BALANCE")
    String errorCode
) {
}
