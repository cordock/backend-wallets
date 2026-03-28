package com.sentbe.wallets.domain.wallet.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sentbe.wallets.domain.wallet.entity.QWalletTransaction;
import com.sentbe.wallets.domain.wallet.entity.WalletTransaction;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WalletTransactionRepositoryCustomImpl implements WalletTransactionRepositoryCustom {

    private static final QWalletTransaction WALLET_TRANSACTION = QWalletTransaction.walletTransaction;

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<WalletTransaction> findAllPage(Long walletId, Pageable pageable) {
        List<WalletTransaction> contents = queryFactory
            .selectFrom(WALLET_TRANSACTION)
            .where(WALLET_TRANSACTION.walletId.eq(walletId))
            .orderBy(WALLET_TRANSACTION.id.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long totalCount = queryFactory
            .select(WALLET_TRANSACTION.count())
            .from(WALLET_TRANSACTION)
            .where(WALLET_TRANSACTION.walletId.eq(walletId))
            .fetchOne();

        long safeTotalCount = totalCount == null ? 0L : totalCount;

        return new PageImpl<>(contents, pageable, safeTotalCount);
    }
}
