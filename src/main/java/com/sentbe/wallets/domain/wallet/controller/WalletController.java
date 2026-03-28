package com.sentbe.wallets.domain.wallet.controller;

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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/{id}/withdrawals")
    public WalletWithdrawalResDto withdrawal(
        @PathVariable long id,
        @Valid @RequestBody WalletWithdrawalReqDto walletWithdrawalReqDto
    ) {
        return walletService.withdrawal(id, walletWithdrawalReqDto);
    }

    @GetMapping("/{id}/transactions")
    public Page<WalletTransactionListDto> getTransactions(
        @PathVariable long id,
        @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        return walletService.getTransactions(id, pageable);
    }
}
