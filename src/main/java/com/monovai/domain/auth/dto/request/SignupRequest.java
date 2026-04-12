package com.monovai.domain.auth.dto.request;

import java.util.List;

import com.monovai.domain.auth.dto.response.OAuthUserInformation;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotNull(message = "사용자 정보가 누락되었습니다") OAuthUserInformation userInformation
) {
}
