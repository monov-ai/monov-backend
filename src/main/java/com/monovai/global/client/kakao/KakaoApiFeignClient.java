package com.monovai.global.client.kakao;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import com.monovai.domain.auth.dto.response.kakao.KakaoUserInformationResponse;

@FeignClient(name = "kakaoFeignClient", url = "https://kapi.kakao.com")
public interface KakaoApiFeignClient {

    @PostMapping(value = "/v2/user/me", consumes = "application/x-www-form-urlencoded;charset=utf-8")
	KakaoUserInformationResponse getInformation(@RequestHeader("Authorization") String accessToken);
}
