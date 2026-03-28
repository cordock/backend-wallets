package com.sentbe.wallets.common.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Sentbe Wallet API",
        description = "월렛 동시 출금 및 잔액 무결성 보장 시스템"
    )
)
public class OpenApiConfig {
}
