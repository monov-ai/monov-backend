package com.monovai.domain.auth.dto.response;



import com.monovai.domain.auth.dto.response.google.GoogleUserInformation;
import com.monovai.domain.auth.dto.response.kakao.KakaoUserInformationResponse;
import com.monovai.domain.auth.entity.enums.SocialType;

import jakarta.validation.constraints.NotEmpty;

public record OAuthUserInformation(
        String socialId,
        SocialType socialType,
        String email,
        @NotEmpty(message = "사용자 닉네임 정보는 필수입니다.") String name,
        String profileImageUrl
) {
    public static OAuthUserInformation from(
            KakaoUserInformationResponse information
    ) {
        return new OAuthUserInformation(
                Long.toString(information.id()),
                SocialType.KAKAO,
                information.kakaoAccount().email(),
                information.kakaoAccount().profile().nickname(),
                information.kakaoAccount().profile().profileImageUrl()
        );
    }

    public static OAuthUserInformation from(
            GoogleUserInformation information
    ) {
        return new OAuthUserInformation(
                information.sub(),
                SocialType.GOOGLE,
                information.email(),
                information.name(),
                information.picture()
        );
    }


}
