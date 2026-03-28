package com.sentbe.wallets.domain.wallet.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sentbe.wallets.domain.wallet.entity.QWallet;
import com.sentbe.wallets.domain.wallet.entity.Wallet;

import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WalletRepositoryCustomImpl implements WalletRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QWallet WALLET =  QWallet.wallet;

    @Override
    public Optional<Wallet> findByIdForUpdate(Long walletId) {
        Wallet wallet = queryFactory
            .selectFrom(WALLET)
            .where(WALLET.id.eq(walletId))
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .fetchOne();

        return Optional.ofNullable(wallet);
    }
}
