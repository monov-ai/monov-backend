package com.monovai.domain.business.edit.dto.response;

public record EditCreatedResponse(Long editId) {

	public static EditCreatedResponse of(Long editId) {
		return new EditCreatedResponse(editId);
	}
}