package com.sentbe.wallets.common.response;

import java.util.Map;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.sentbe.wallets.common.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"status", "code", "message", "data"})
public class CustomResponse<T> {

    private final int status;
    private final int code;
    private final String message;
    private final T data;

    @SuppressWarnings("unchecked")
    public CustomResponse() {
        this.status = HttpStatus.OK.value();
        this.code = ErrorCode.SUCCESS.getCode();
        this.message = ErrorCode.SUCCESS.getMessage();
        this.data = (T) Map.of();
    }

    public CustomResponse(T data) {
        this.status = HttpStatus.OK.value();
        this.code = ErrorCode.SUCCESS.getCode();
        this.message = ErrorCode.SUCCESS.getMessage();
        this.data = data;
    }

    @SuppressWarnings("unchecked")
    public CustomResponse(HttpStatus status, int code, String message) {
        this.status = status.value();
        this.code = code;
        this.message = message;
        this.data = (T) Map.of();
    }

    public CustomResponse(HttpStatus status, int code, String message, T data) {
        this.status = status.value();
        this.code = code;
        this.message = message;
        this.data = data;
    }
}
