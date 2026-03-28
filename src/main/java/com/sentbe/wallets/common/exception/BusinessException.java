package com.sentbe.wallets.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ResponseCode responseCode;

    public BusinessException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.responseCode = responseCode;
    }
}
