package com.sentbe.wallets.domain.wallet.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sentbe.wallets.common.enums.WalletTransactionStatus;
import com.sentbe.wallets.common.exception.BusinessException;
import com.sentbe.wallets.common.exception.ResponseCode;
import com.sentbe.wallets.domain.wallet.dto.WalletWithdrawalReqDto;
import com.sentbe.wallets.domain.wallet.dto.WalletWithdrawalResDto;
import com.sentbe.wallets.domain.wallet.entity.Wallet;
import com.sentbe.wallets.domain.wallet.entity.WalletTransaction;
import com.sentbe.wallets.domain.wallet.fixture.WalletFixture;
import com.sentbe.wallets.domain.wallet.repository.WalletRepository;
import com.sentbe.wallets.domain.wallet.repository.WalletTransactionRepository;

import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
class WalletConcurrencyIntegrationTest {

    private static final long DEFAULT_INITIAL_BALANCE = 100_000L;
    private static final long WITHDRAW_AMOUNT = 1_000L;
    private static final int THREAD_COUNT = 100;
    private static final int FAILURE_THREAD_COUNT = 20;

    private static final String PREFIX_TRANSACTION_ID = "TX_";
    private static final String TX_DATE = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private WalletFixture walletFixture;

    private Long walletId;

    @BeforeEach
    void setUp() {
        walletId = null;
    }

    @AfterEach
    void tearDown() {
        if (walletId == null)
            return;
        walletTransactionRepository.deleteByWalletId(walletId);
        walletRepository.deleteById(walletId);
    }

    @Test
    @DisplayName("동일 transactionId 동시 요청 시 락 획득 대기 후에도 추가 차감 없이 기존 성공 결과를 재현한다")
    void Given_SameTransactionId_When_ConcurrentWithdrawals_Then_AllRequestsReturnSameSuccessResult() throws Exception {
        // Given
        walletId = createWallet(DEFAULT_INITIAL_BALANCE);
        long expectedBalance = DEFAULT_INITIAL_BALANCE - WITHDRAW_AMOUNT;

        // When
        ConcurrencyResult result = runConcurrentWithdrawals(walletId, THREAD_COUNT, WITHDRAW_AMOUNT, true);

        // Then
        SoftAssertions softly = new SoftAssertions();

        // 1. 응답 멱등성
        softly.assertThat(result.responses()).hasSize(THREAD_COUNT);
        softly.assertThat(result.responses()).allSatisfy(res -> {
            softly.assertThat(res.status()).isEqualTo(WalletTransactionStatus.SUCCESS);
            softly.assertThat(res.remainBalance()).isEqualTo(expectedBalance);
        });

        // 2. 데이터 정합성
        List<WalletTransaction> txs = walletTransactionRepository.findAllByWalletId(walletId);
        softly.assertThat(txs).hasSize(1);

        // 3. 최종잔액 확인
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        softly.assertThat(wallet.getBalance()).isEqualTo(expectedBalance);

        softly.assertAll();

        log.info(
            "성공={}, 잔액부족실패={}, 예상외실패={}, 적재(SUCCESS)=1, 적재(FAILED)=0, 최종잔액={}",
            result.successCount(),
            result.insufficientBalanceFailCount(),
            result.unexpectedFailCount(),
            wallet.getBalance()
        );
    }

    @Test
    @DisplayName("동일 transactionId 동시 요청에서 잔액 부족이면 모두 동일 실패 결과를 재현한다")
    void Given_SameTransactionId_When_ConcurrentWithdrawalsWithInsufficientBalance_Then_AllRequestsReturnSameFailureResult()
        throws Exception {
        // Given
        walletId = createWallet(DEFAULT_INITIAL_BALANCE);
        long overWithdrawAmount = DEFAULT_INITIAL_BALANCE + WITHDRAW_AMOUNT;

        // When
        ConcurrencyResult result = runConcurrentWithdrawals(walletId, FAILURE_THREAD_COUNT, overWithdrawAmount, true);

        // Then
        SoftAssertions softly = new SoftAssertions();

        // 1. 응답 멱등성(실패는 예외로 처리되어 성공 응답 없음)
        softly.assertThat(result.responses()).isEmpty();
        softly.assertThat(result.successCount()).isEqualTo(0);
        softly.assertThat(result.failCount()).isEqualTo(FAILURE_THREAD_COUNT);
        softly.assertThat(result.insufficientBalanceFailCount() + result.unexpectedFailCount())
            .isEqualTo(FAILURE_THREAD_COUNT);

        // 2. 데이터 정합성
        List<WalletTransaction> txs = walletTransactionRepository.findAllByWalletId(walletId);
        softly.assertThat(txs).hasSize(1);

        WalletTransaction tx = txs.get(0);
        softly.assertThat(tx.getStatus()).isEqualTo(WalletTransactionStatus.FAILED);
        softly.assertThat(tx.getAmount()).isEqualTo(overWithdrawAmount);
        softly.assertThat(tx.getRemainBalance()).isEqualTo(DEFAULT_INITIAL_BALANCE);

        // 3. 최종잔액 확인
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        softly.assertThat(wallet.getBalance()).isEqualTo(DEFAULT_INITIAL_BALANCE);

        softly.assertAll();

        log.info(
            "성공={}, 잔액부족실패={}, 예상외실패={}, 적재(SUCCESS)=0, 적재(FAILED)=1, 최종잔액={}",
            result.successCount(),
            result.insufficientBalanceFailCount(),
            result.unexpectedFailCount(),
            wallet.getBalance()
        );
    }

    @Test
    @DisplayName("서로 다른 transactionId 동시 요청 시 전액 출금 가능한 경우 모두 성공한다")
    void Given_DifferentTransactionIds_When_ConcurrentWithdrawalsWithEnoughBalance_Then_AllRequestsSucceed() throws
        Exception {
        // Given
        walletId = createWallet(DEFAULT_INITIAL_BALANCE);

        // When
        ConcurrencyResult result = runConcurrentWithdrawals(walletId, THREAD_COUNT, WITHDRAW_AMOUNT, false);

        // Then
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        List<WalletTransaction> txs = walletTransactionRepository.findAllByWalletId(walletId);

        long successTxCount = txs.stream()
            .filter(tx -> tx.getStatus() == WalletTransactionStatus.SUCCESS)
            .count();

        long failedTxCount = txs.stream()
            .filter(tx -> tx.getStatus() == WalletTransactionStatus.FAILED)
            .count();

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(txs).hasSize(THREAD_COUNT);
        softly.assertThat(successTxCount).isEqualTo(THREAD_COUNT);
        softly.assertThat(failedTxCount).isEqualTo(0);
        softly.assertThat(wallet.getBalance()).isEqualTo(0L);
        softly.assertAll();

        log.info(
            "성공={}, 잔액부족실패={}, 예상외실패={}, 적재(SUCCESS)={}, 적재(FAILED)={}, 최종잔액={}",
            result.successCount(),
            result.insufficientBalanceFailCount(),
            result.unexpectedFailCount(),
            successTxCount,
            failedTxCount,
            wallet.getBalance()
        );
    }

    @Test
    @DisplayName("서로 다른 transactionId 동시 요청 시 잔액 부족 시점부터는 실패하며 음수 잔액이 되지 않는다")
    void Given_DifferentTransactionIds_When_ConcurrentWithdrawalsWithLimitedBalance_Then_BalanceNeverNegative() throws Exception {
        // Given
        long limitedInitialBalance = 5_000L;
        int expectedSuccess = 5;
        int expectedFail = FAILURE_THREAD_COUNT - expectedSuccess;

        walletId = createWallet(limitedInitialBalance);

        // When
        ConcurrencyResult result = runConcurrentWithdrawals(walletId, FAILURE_THREAD_COUNT, WITHDRAW_AMOUNT, false);

        // Then
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();

        List<WalletTransaction> txs = walletTransactionRepository.findAllByWalletId(walletId);

        long successTxCount = txs
            .stream().filter(tx -> tx.getStatus() == WalletTransactionStatus.SUCCESS)
            .count();

        long failedTxCount = txs.stream()
            .filter(tx -> tx.getStatus() == WalletTransactionStatus.FAILED)
            .count();

        SoftAssertions softly = new SoftAssertions();

        // DB 트랜잭션 정합성 검증
        softly.assertThat(txs).hasSize(FAILURE_THREAD_COUNT);
        softly.assertThat(successTxCount).isEqualTo(expectedSuccess);
        softly.assertThat(failedTxCount).isEqualTo(expectedFail);

        // 최종 잔액 검증
        softly.assertThat(wallet.getBalance()).isEqualTo(0L);
        softly.assertThat(successTxCount * WITHDRAW_AMOUNT + wallet.getBalance()).isEqualTo(limitedInitialBalance);

        softly.assertAll();

        log.info(
            "성공={}, 잔액부족실패={}, 예상외실패={}, 적재(SUCCESS)={}, 적재(FAILED)={}, 최종잔액={}",
            result.successCount(),
            result.insufficientBalanceFailCount(),
            result.unexpectedFailCount(),
            successTxCount,
            failedTxCount,
            wallet.getBalance()
        );

        if (!result.unexpectedFailures().isEmpty()) {
            log.warn("[예상외 실패 발생] 상세 사유: {}", result.unexpectedFailures());
        }
    }

    private ConcurrencyResult runConcurrentWithdrawals(
        Long walletId, int threadCount, long amount, boolean isSameTxId
    ) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger insufficientCount = new AtomicInteger();
        AtomicInteger unexpectedCount = new AtomicInteger();
        Queue<String> unexpectedFailures = new ConcurrentLinkedQueue<>();
        Queue<WalletWithdrawalResDto> responses = new ConcurrentLinkedQueue<>();

        String sharedTransactionId = buildTxId(0);

        try {
            for (int i = 0; i < threadCount; i++) {
                final int index = i;

                executorService.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();

                        String transactionId = isSameTxId ? sharedTransactionId : buildTxId(index);

                        WalletWithdrawalResDto response = walletService.withdrawal(
                            walletId,
                            new WalletWithdrawalReqDto(amount, transactionId)
                        );
                        responses.add(response);
                        successCount.incrementAndGet();
                    } catch (BusinessException e) {
                        if (e.getResponseCode() == ResponseCode.INSUFFICIENT_BALANCE) {
                            insufficientCount.incrementAndGet();
                        } else {
                            unexpectedCount.incrementAndGet();
                            unexpectedFailures.add(e.getMessage());
                        }
                    } catch (Throwable t) {
                        unexpectedCount.incrementAndGet();
                        unexpectedFailures.add(t.getClass().getSimpleName() + ": " + t.getMessage());
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            assertTrue(readyLatch.await(10, TimeUnit.SECONDS), "스레드 준비 타임아웃");
            startLatch.countDown();
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "테스트 실행 타임아웃");
        } finally {
            executorService.shutdownNow();
        }

        return new ConcurrencyResult(
            successCount.get(),
            insufficientCount.get() + unexpectedCount.get(),
            insufficientCount.get(),
            unexpectedCount.get(),
            List.copyOf(unexpectedFailures),
            List.copyOf(responses)
        );
    }

    private Long createWallet(long balance) {
        return walletFixture.create(System.nanoTime(), balance).getId();
    }

    private static String buildTxId(int index) {
        return PREFIX_TRANSACTION_ID + TX_DATE + "_" + String.format("%05d", index);
    }

    private record ConcurrencyResult(
        int successCount,
        int failCount,
        int insufficientBalanceFailCount,
        int unexpectedFailCount,
        List<String> unexpectedFailures,
        List<WalletWithdrawalResDto> responses
    ) {
    }
}
