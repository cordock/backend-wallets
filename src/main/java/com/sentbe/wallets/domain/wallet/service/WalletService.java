package com.sentbe.wallets.domain.wallet.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sentbe.wallets.common.enums.WalletTransactionErrorCode;
import com.sentbe.wallets.common.enums.WalletTransactionStatus;
import com.sentbe.wallets.common.exception.BusinessException;
import com.sentbe.wallets.common.exception.ResponseCode;
import com.sentbe.wallets.domain.wallet.dto.WalletTransactionListDto;
import com.sentbe.wallets.domain.wallet.dto.WalletWithdrawalReqDto;
import com.sentbe.wallets.domain.wallet.dto.WalletWithdrawalResDto;
import com.sentbe.wallets.domain.wallet.entity.Wallet;
import com.sentbe.wallets.domain.wallet.entity.WalletTransaction;
import com.sentbe.wallets.domain.wallet.repository.WalletRepository;
import com.sentbe.wallets.domain.wallet.repository.WalletTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletTransactionService walletTransactionService;

    /**
     * 월렛 출금
     * @param id 월렛 ID
     * @param walletWithdrawalReqDto 월렛 출금 요청 DTO
     * @return
     */
    @Transactional
    public WalletWithdrawalResDto withdrawal(Long id, WalletWithdrawalReqDto walletWithdrawalReqDto) {
        Long amount = walletWithdrawalReqDto.amount();
        String transactionId = walletWithdrawalReqDto.transactionId();
        LocalDateTime requestedAt = LocalDateTime.now();

        // 1. 멱등성 체크 (early return)
        WalletTransaction walletTransaction = getWalletTransaction(id, transactionId);

        if (walletTransaction != null) {
            return handleExistingTransaction(walletTransaction);
        }

        // 2. wallet 조회 (Pessimistic Lock)
        Wallet wallet = walletRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new BusinessException(ResponseCode.NOT_FOUND_WALLET));

        // 3. 락 획득 후 멱등성 재확인
        WalletTransaction lockedExistingTransaction = getWalletTransaction(id, transactionId);

        if (lockedExistingTransaction != null) {
            return handleExistingTransaction(lockedExistingTransaction);
        }

        // 4. 잔액 검증
        if (wallet.getBalance() < amount) {
            walletTransactionService.saveFailedWithdrawal(
                wallet,
                walletWithdrawalReqDto,
                requestedAt,
                WalletTransactionErrorCode.INSUFFICIENT_BALANCE
            );

            throw new BusinessException(ResponseCode.INSUFFICIENT_BALANCE);
        }

        // 5. 출금 처리
        wallet.decreaseBalance(amount);

        // 6. 성공 transaction 저장
        WalletTransaction successTransaction = WalletTransaction.ofWithdraw(
            id,
            transactionId,
            WalletTransactionStatus.SUCCESS,
            amount,
            requestedAt,
            wallet.getBalance(),
            null
        );

        walletTransactionRepository.save(successTransaction);

        return toResponse(successTransaction);
    }

    /**
     * 월렛 입출금 내역 조회
     * @param id 월렛 ID
     * @return
     */
    @Transactional(readOnly = true)
    public Page<WalletTransactionListDto> getTransactions(Long id, Pageable pageable) {
        if (!walletRepository.existsById(id)) {
            throw new BusinessException(ResponseCode.NOT_FOUND_WALLET);
        }

        return walletTransactionRepository.findAllPage(id, pageable)
            .map(WalletTransactionListDto::from);
    }

    /**
     * 기존 입출금 내역 기반 응답 처리
     * @param walletTransaction 기존 입출금 내역
     * @return
     */
    private WalletWithdrawalResDto handleExistingTransaction(WalletTransaction walletTransaction) {
        if (walletTransaction.getStatus() == WalletTransactionStatus.FAILED) {
            if (walletTransaction.getErrorCode() != null) {
                throw new BusinessException(walletTransaction.getErrorCode().toResponseCode());
            }
            throw new BusinessException(ResponseCode.INSUFFICIENT_BALANCE);
        }

        return toResponse(walletTransaction);
    }

    private WalletTransaction getWalletTransaction(Long walletId, String transactionId) {
        return walletTransactionRepository
            .findByWalletIdAndTransactionId(walletId, transactionId)
            .orElse(null);
    }

    private WalletWithdrawalResDto toResponse(WalletTransaction walletTransaction) {
        String errorCode = walletTransaction.getErrorCode() == null ? null : walletTransaction.getErrorCode().name();

        return new WalletWithdrawalResDto(
            walletTransaction.getWalletId(),
            walletTransaction.getTransactionId(),
            walletTransaction.getStatus(),
            walletTransaction.getAmount(),
            walletTransaction.getRemainBalance(),
            errorCode
        );
    }

}
