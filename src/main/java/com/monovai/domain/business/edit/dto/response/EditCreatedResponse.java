package com.monovai.domain.business.edit.dto.response;

public record EditCreatedResponse(String editId) {

	public static EditCreatedResponse of(String editId) {
		return new EditCreatedResponse(editId);
	}
}
