package com.monovai.global.error.exception;

import com.monovai.global.error.code.ErrorCode;

public class InvalidValueException extends BusinessException {
	public InvalidValueException(ErrorCode errorCode) {
		super(errorCode);
	}
}
