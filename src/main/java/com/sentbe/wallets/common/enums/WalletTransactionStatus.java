package com.sentbe.wallets.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WalletTransactionStatus {

    SUCCESS("성공"),
    FAILED("실패");

    private final String description;
}
