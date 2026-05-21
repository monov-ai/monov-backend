package com.monovai.global.error.exception;

import com.monovai.global.error.code.ErrorCode;

import lombok.Getter;

/**
 * 429 Too Many Requests. retryAfterSeconds 는 Retry-After 헤더로 노출.
 */
@Getter
public class RateLimitException extends RuntimeException {
	private final ErrorCode errorCode;
	private final long retryAfterSeconds;

	public RateLimitException(long retryAfterSeconds) {
		super(ErrorCode.RATE_LIMITED.getMessage());
		this.errorCode = ErrorCode.RATE_LIMITED;
		this.retryAfterSeconds = retryAfterSeconds;
	}
}
