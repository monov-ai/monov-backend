package com.monovai.global.error.exception;

import com.monovai.global.error.code.ErrorCode;

public class NotFoundException extends BusinessException {
	public NotFoundException(final ErrorCode errorCode) {
		super(errorCode);
	}
}
