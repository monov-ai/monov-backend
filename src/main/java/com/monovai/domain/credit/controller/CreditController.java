package com.monovai.domain.credit.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monovai.domain.credit.dto.response.CreditBalanceResponse;
import com.monovai.domain.credit.dto.response.CreditLedgerResponse;
import com.monovai.domain.credit.repository.CreditLedgerRepository;
import com.monovai.domain.credit.service.CreditService;
import com.monovai.global.error.code.SuccessCode;
import com.monovai.global.error.dto.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Credits", description = "크레딧 잔액 / 사용 내역")
@RestController
@RequestMapping("/api/v1/credits")
@RequiredArgsConstructor
public class CreditController {

	private final CreditService creditService;
	private final CreditLedgerRepository ledgerRepository;

	@GetMapping("/balance")
	@Operation(summary = "크레딧 잔액 조회")
	public ResponseEntity<SuccessResponse<CreditBalanceResponse>> balance(
		@AuthenticationPrincipal Long userId
	) {
		return ResponseEntity.ok(
			SuccessResponse.of(SuccessCode.SUCCESS_FETCH, CreditBalanceResponse.of(creditService.getWallet(userId))));
	}

	@GetMapping("/ledger")
	@Operation(summary = "크레딧 변동 내역")
	public ResponseEntity<SuccessResponse<CreditLedgerResponse>> ledger(
		@AuthenticationPrincipal Long userId,
		@RequestParam(value = "limit", required = false, defaultValue = "50") int limit
	) {
		int capped = Math.min(Math.max(limit, 1), 200);
		return ResponseEntity.ok(SuccessResponse.of(SuccessCode.SUCCESS_FETCH,
			CreditLedgerResponse.of(
				ledgerRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, capped)))));
	}
}
