package com.sentbe.wallets.domain.wallet.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sentbe.wallets.domain.wallet.entity.Wallet;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long>, WalletRepositoryCustom {}
