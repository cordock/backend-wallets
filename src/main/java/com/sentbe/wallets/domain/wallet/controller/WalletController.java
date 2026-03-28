package com.sentbe.wallets.domain.wallet.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sentbe.wallets.domain.wallet.dto.WalletTransactionListDto;
import com.sentbe.wallets.domain.wallet.dto.WalletWithdrawalReqDto;
import com.sentbe.wallets.domain.wallet.dto.WalletWithdrawalResDto;
import com.sentbe.wallets.domain.wallet.service.WalletService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
@Tag(name = "월렛 관리")
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/{id}/withdrawals")
    @Operation(
        summary = "월렛 출금",
        description = "월렛 출금 시 사용합니다."
    )
    public WalletWithdrawalResDto withdrawal(
        @PathVariable long id,
        @Valid @RequestBody WalletWithdrawalReqDto walletWithdrawalReqDto
    ) {
        return walletService.withdrawal(id, walletWithdrawalReqDto);
    }

    @GetMapping("/{id}/transactions")
    @Operation(
        summary = "월렛 입출금 내역 조회",
        description = """
            월렛 입출금 내역 조회 시 사용합니다.
            
            - 정렬 조건: id (desc)
            """
    )
    public Page<WalletTransactionListDto> getTransactions(
        @PathVariable long id,
        @ParameterObject @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        return walletService.getTransactions(id, pageable);
    }
}
