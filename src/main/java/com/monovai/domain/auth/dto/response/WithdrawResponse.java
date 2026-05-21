package com.monovai.domain.auth.dto.response;

import java.time.Instant;

public record WithdrawResponse(
	Instant deletedAt,
	int dataRetentionDays,
	int pendingResources
) {
	public static WithdrawResponse of(int pendingResources) {
		return new WithdrawResponse(Instant.now(), 30, pendingResources);
	}
}
