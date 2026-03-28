package com.sentbe.wallets.domain.wallet.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sentbe.wallets.common.enums.WalletTransactionErrorCode;
import com.sentbe.wallets.common.enums.WalletTransactionStatus;
import com.sentbe.wallets.domain.wallet.dto.WalletWithdrawalReqDto;
import com.sentbe.wallets.domain.wallet.entity.Wallet;
import com.sentbe.wallets.domain.wallet.entity.WalletTransaction;
import com.sentbe.wallets.domain.wallet.repository.WalletTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletTransactionService {

    private final WalletTransactionRepository walletTransactionRepository;

    /**
     * 출금 실패 처리 (별도 트랜잭션)
     * - 잔액 부족 등으로 출금이 실패한 경우 해당 이력 기록
     * - 상위 트랜잭션이 롤백되더라도 실패 이력 유지
     * - 멱등성 체크 이후 호출 필수
     * @param wallet 해당 요청의 월렛 정보
     * @param walletWithdrawalReqDto 요청 정보 DTO
     * @param requestedAt 요청 시각
     * @param errorCode 실패 에러코드
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void saveFailedWithdrawal(
        Wallet wallet,
        WalletWithdrawalReqDto walletWithdrawalReqDto,
        LocalDateTime requestedAt,
        WalletTransactionErrorCode errorCode
    ) {
        Long walletId = wallet.getId();
        String transactionId = walletWithdrawalReqDto.transactionId();
        Long amount = walletWithdrawalReqDto.amount();
        Long remainBalance = wallet.getBalance();

        WalletTransaction failedTransaction = WalletTransaction.ofWithdraw(
            walletId,
            transactionId,
            WalletTransactionStatus.FAILED,
            amount,
            requestedAt,
            remainBalance,
            errorCode
        );

        walletTransactionRepository.saveAndFlush(failedTransaction);
    }
}
