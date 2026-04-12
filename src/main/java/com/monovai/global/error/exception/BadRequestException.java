package com.monovai.global.error.exception;

import com.monovai.global.error.code.ErrorCode;

public class BadRequestException extends BusinessException {
	public BadRequestException() {
		super(ErrorCode.BAD_REQUEST_DATA);
	}

	public BadRequestException(ErrorCode errorCode) {
		super(errorCode);
	}
}

