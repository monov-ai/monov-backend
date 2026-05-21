package com.monovai.domain.credit.dto.response;

import java.time.Instant;
import java.util.List;

import com.monovai.domain.credit.entity.CreditLedger;

public record CreditLedgerResponse(
	List<Entry> entries
) {
	public record Entry(
		String type,
		int amount,
		int balanceAfter,
		String reason,
		String jobId,
		Instant createdAt
	) {
		public static Entry of(CreditLedger l) {
			return new Entry(
				l.getType().getValue(),
				l.getAmount(),
				l.getBalanceAfter(),
				l.getReason(),
				l.getJobId(),
				l.getCreatedAt() != null ? l.getCreatedAt().toInstant() : null
			);
		}
	}

	public static CreditLedgerResponse of(List<CreditLedger> ledgers) {
		return new CreditLedgerResponse(ledgers.stream().map(Entry::of).toList());
	}
}
