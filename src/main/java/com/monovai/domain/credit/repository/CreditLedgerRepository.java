package com.monovai.domain.credit.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.credit.entity.CreditLedger;
import com.monovai.domain.credit.entity.enums.CreditTxnType;

public interface CreditLedgerRepository extends JpaRepository<CreditLedger, Long> {

	boolean existsByJobIdAndType(String jobId, CreditTxnType type);

	List<CreditLedger> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
