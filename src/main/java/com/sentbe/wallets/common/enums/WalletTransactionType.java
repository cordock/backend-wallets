package com.sentbe.wallets.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WalletTransactionType {

    WITHDRAW("출금"),
    DEPOSIT("입금");

    private final String description;
}
