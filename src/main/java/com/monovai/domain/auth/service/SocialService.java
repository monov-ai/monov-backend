package com.monovai.domain.auth.service;

import com.monovai.domain.auth.dto.response.LoginUriResponse;
import com.monovai.domain.auth.dto.response.OAuthUserInformation;

public interface SocialService {
    public LoginUriResponse getAuthorizationUri();

    public OAuthUserInformation getUserInfo(String code);

    public OAuthUserInformation getUserInfoByAccessToken(String token);
}
