package com.sentbe.wallets.common.response;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.sentbe.wallets.common.exception.ResponseCode;

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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private CustomPageInfo pageInfo;

    @SuppressWarnings("unchecked")
    public CustomResponse() {
        this.status = HttpStatus.OK.value();
        this.code = ResponseCode.SUCCESS.getCode();
        this.message = ResponseCode.SUCCESS.getMessage();
        this.data = (T) Map.of();
        this.pageInfo = null;
    }

    public CustomResponse(T data) {
        this.status = HttpStatus.OK.value();
        this.code = ResponseCode.SUCCESS.getCode();
        this.message = ResponseCode.SUCCESS.getMessage();
        this.data = data;
        this.pageInfo = null;
    }

    @SuppressWarnings("unchecked")
    public CustomResponse(Page<?> page) {
        this.status = HttpStatus.OK.value();
        this.code = ResponseCode.SUCCESS.getCode();
        this.message = ResponseCode.SUCCESS.getMessage();
        this.data = (T) page.getContent();
        this.pageInfo = CustomPageInfo.from(page);
    }

    @SuppressWarnings("unchecked")
    public CustomResponse(HttpStatus status, int code, String message) {
        this.status = status.value();
        this.code = code;
        this.message = message;
        this.data = (T) Map.of();
        this.pageInfo = null;
    }

    public CustomResponse(HttpStatus status, int code, String message, T data) {
        this.status = status.value();
        this.code = code;
        this.message = message;
        this.data = data;
        this.pageInfo = null;
    }
}
