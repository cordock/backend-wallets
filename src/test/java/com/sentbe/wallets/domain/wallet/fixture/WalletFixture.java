package com.sentbe.wallets.domain.wallet.fixture;

import org.springframework.stereotype.Component;

import com.sentbe.wallets.domain.wallet.entity.Wallet;
import com.sentbe.wallets.domain.wallet.repository.WalletRepository;

@Component
public class WalletFixture {

    private final WalletRepository walletRepository;

    public WalletFixture(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Wallet create(Long memberId, Long balance) {
        return walletRepository.saveAndFlush(Wallet.of(memberId, balance));
    }
}
