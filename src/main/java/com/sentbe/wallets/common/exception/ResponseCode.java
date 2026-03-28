package com.sentbe.wallets.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResponseCode {

    SUCCESS(HttpStatus.OK, 200, "요청이 성공적으로 처리되었습니다."),

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, 4000, "입력값 검증에 실패했습니다."),
    NOT_FOUND_WALLET(HttpStatus.NOT_FOUND, 4001, "월렛이 존재하지 않습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.CONFLICT, 4002, "잔액이 부족하여 출금이 불가능합니다."),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 5000, "내부 서버 에러가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;
}
