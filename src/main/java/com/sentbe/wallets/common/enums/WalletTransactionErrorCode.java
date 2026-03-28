package com.sentbe.wallets.common.enums;

import com.sentbe.wallets.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WalletTransactionErrorCode {

    INSUFFICIENT_BALANCE(ErrorCode.INSUFFICIENT_BALANCE);

    private final ErrorCode errorCode;

    public ErrorCode toErrorCode() {
        return errorCode;
    }
}
