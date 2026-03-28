package com.sentbe.wallets.domain.wallet.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.sentbe.wallets.common.enums.WalletTransactionErrorCode;
import com.sentbe.wallets.common.enums.WalletTransactionStatus;
import com.sentbe.wallets.common.enums.WalletTransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "wallet_transaction",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_wallet_id_transaction_id",
            columnNames = {"wallet_id", "transaction_id"}
        )
    }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    private WalletTransactionType type;

    @Enumerated(EnumType.STRING)
    private WalletTransactionStatus status;

    private Long amount;

    private Long remainBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_code")
    private WalletTransactionErrorCode errorCode;

    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static WalletTransaction ofWithdraw(
        Long walletId,
        String transactionId,
        WalletTransactionStatus status,
        Long amount,
        LocalDateTime requestedAt,
        Long remainBalance,
        WalletTransactionErrorCode errorCode
    ) {
        WalletTransactionErrorCode code = status == WalletTransactionStatus.FAILED
            ? errorCode : null;

        return WalletTransaction.builder()
            .walletId(walletId)
            .transactionId(transactionId)
            .type(WalletTransactionType.WITHDRAW)
            .status(status)
            .amount(amount)
            .requestedAt(requestedAt)
            .remainBalance(remainBalance)
            .errorCode(code)
            .build();
    }
}
