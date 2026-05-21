package com.monovai.global.error.dto;

import java.util.List;
import java.util.Set;

import org.springframework.validation.BindingResult;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.monovai.global.error.code.ErrorCode;

import jakarta.validation.ConstraintViolation;

/**
 * 실패 응답. monov-web SDK 계약에 맞춰 {@code { status, message, data }} envelope 로 통일.
 * - data.code: enum-like 문자열 코드 (예: USER_NOT_FOUND, INSUFFICIENT_CREDIT)
 * - data.fieldErrors: 검증 실패 상세 [{ field, code, message }]
 * - data.detail: 부가 정보 (선택)
 */
@JsonPropertyOrder({"status", "message", "data"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
	int status,
	String message,
	ErrorData data
) {
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ErrorData(
		String code,
		List<FieldError> fieldErrors,
		String detail
	) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record FieldError(
		String field,
		String code,
		String message
	) {
		public static List<FieldError> of(BindingResult bindingResult) {
			return bindingResult.getFieldErrors().stream()
				.map(error -> new FieldError(
					error.getField(),
					error.getCode(),
					error.getDefaultMessage()
				))
				.toList();
		}

		public static List<FieldError> of(Set<ConstraintViolation<?>> violations) {
			return violations == null ? List.of() : violations.stream()
				.map(v -> new FieldError(
					v.getPropertyPath().toString(),
					v.getConstraintDescriptor() != null
						? v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName() : null,
					v.getMessage()
				))
				.toList();
		}
	}

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(
			errorCode.getHttpStatus().value(),
			errorCode.getMessage(),
			new ErrorData(errorCode.name(), null, null)
		);
	}

	public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult) {
		return new ErrorResponse(
			errorCode.getHttpStatus().value(),
			errorCode.getMessage(),
			new ErrorData(errorCode.name(), FieldError.of(bindingResult), null)
		);
	}

	public static ErrorResponse of(ErrorCode errorCode, Set<ConstraintViolation<?>> violations) {
		return new ErrorResponse(
			errorCode.getHttpStatus().value(),
			errorCode.getMessage(),
			new ErrorData(errorCode.name(), FieldError.of(violations), null)
		);
	}

	public static ErrorResponse of(ErrorCode errorCode, Object detail) {
		return new ErrorResponse(
			errorCode.getHttpStatus().value(),
			errorCode.getMessage(),
			new ErrorData(errorCode.name(), null, detail != null ? detail.toString() : null)
		);
	}
}
