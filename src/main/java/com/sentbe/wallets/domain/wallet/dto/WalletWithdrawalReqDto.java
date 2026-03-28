package com.sentbe.wallets.domain.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(name = "WalletWithdrawalReqDto", description = "월렛 출금 요청 모델")
public record WalletWithdrawalReqDto(

    @NotNull
    @Positive
    @Schema(description = "월렛 출금 요청 금액", example = "10000")
    Long amount,

    @NotBlank
    @Schema(description = "월렛 출금 거래 ID", example = "TX_20260101_00001")
    String transactionId
) {
}
