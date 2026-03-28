package com.sentbe.wallets.common.enums;

import com.sentbe.wallets.common.exception.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WalletTransactionErrorCode {

    INSUFFICIENT_BALANCE(ResponseCode.INSUFFICIENT_BALANCE);

    private final ResponseCode responseCode;

    public ResponseCode toResponseCode() {
        return responseCode;
    }
}
