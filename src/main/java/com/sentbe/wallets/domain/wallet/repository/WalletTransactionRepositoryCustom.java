package com.sentbe.wallets.domain.wallet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sentbe.wallets.domain.wallet.entity.WalletTransaction;

public interface WalletTransactionRepositoryCustom {

    Page<WalletTransaction> findAllPage(Long walletId, Pageable pageable);
}
