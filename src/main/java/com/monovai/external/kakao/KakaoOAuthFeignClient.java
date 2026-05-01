package com.monovai.external.kakao;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.monovai.domain.auth.dto.response.kakao.KakaoOAuthResponse;

@FeignClient(value = "kakaoOAuthFeignClient", url = "https://kauth.kakao.com/oauth")
public interface KakaoOAuthFeignClient {

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	KakaoOAuthResponse getToken(
            @RequestParam("grant_type") String grantType,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("code") String code
    );
}
