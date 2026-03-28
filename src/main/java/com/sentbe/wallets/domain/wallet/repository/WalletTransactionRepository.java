package com.sentbe.wallets.domain.wallet.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sentbe.wallets.domain.wallet.entity.WalletTransaction;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Optional<WalletTransaction> findByWalletIdAndTransactionId(Long walletId, String transactionId);
}
