package com.sentbe.wallets.domain.wallet.repository;

import java.util.Optional;

import com.sentbe.wallets.domain.wallet.entity.Wallet;

public interface WalletRepositoryCustom {

    Optional<Wallet> findByIdForUpdate(Long walletId);
}
