package com.monovai.domain.auth.dto.response;

import com.monovai.domain.auth.dto.response.kakao.KakaoUserInformationResponse;

public record UserInformation(
        String email,
        String name,
        String profileImageUrl
) {
    public static UserInformation from(
            KakaoUserInformationResponse information
    ) {
        return new UserInformation(
                information.kakaoAccount().email(),
                information.kakaoAccount().profile().nickname(),
                information.kakaoAccount().profile().profileImageUrl()
        );
    }
}
