package com.monovai.domain.auth.dto.response;

public record JwtResponse(String accessToken, String refreshToken) {

    public static JwtResponse of(String accessToken, String refreshToken) {
        return new JwtResponse(accessToken, refreshToken);
    }
}
