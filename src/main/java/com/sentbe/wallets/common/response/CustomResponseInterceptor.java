package com.sentbe.wallets.common.response;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.sentbe")
public class CustomResponseInterceptor implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return !CustomResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public @Nullable Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType,
        MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
        ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof CustomResponse) {
            return body;
        }

        if (body == null) {
            return new CustomResponse<>();
        }

        return new CustomResponse<>(body);
    }
}
