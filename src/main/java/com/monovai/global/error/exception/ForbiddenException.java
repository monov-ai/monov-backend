package com.monovai.global.error.exception;

import com.monovai.global.error.code.ErrorCode;

public class ForbiddenException extends BusinessException {
	public ForbiddenException(ErrorCode errorCode) {
		super(errorCode);
	}
}

