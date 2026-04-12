package com.monovai.global.error.exception;

import com.monovai.global.error.code.ErrorCode;

public class UnauthorizedException extends BusinessException {
	public UnauthorizedException() {
		super(ErrorCode.UNAUTHORIZED);
	}

	public UnauthorizedException(ErrorCode errorCode) {
		super(errorCode);
	}

}
