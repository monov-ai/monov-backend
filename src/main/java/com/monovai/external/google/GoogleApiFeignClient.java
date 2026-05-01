package com.monovai.external.google;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import com.monovai.domain.auth.dto.response.google.GoogleUserInformation;

@FeignClient(name = "googleUserInfoFeignClient", url = "https://www.googleapis.com")
public interface GoogleApiFeignClient {

    @GetMapping(value = "/oauth2/v3/userinfo")
	GoogleUserInformation getUserInfo(
            @RequestHeader("Authorization") String accessToken
    );
}
