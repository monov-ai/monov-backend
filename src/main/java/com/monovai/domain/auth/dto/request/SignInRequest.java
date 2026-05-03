package com.monovai.domain.auth.dto.request;

import jakarta.validation.constraints.NotEmpty;

public record SignInRequest(
        @NotEmpty(message = "인가코드가 필요합니다.") String code,
        @NotEmpty(message = "socialType을 반드시 지정해주어야 합니다.") String socialType) {
}
