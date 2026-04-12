package com.monovai.domain.auth.dto.request.google;

public record GoogleTokenRequest(
        String code,
        String client_id,
        String client_secret,
        String redirect_uri,
        String grant_type
) {
}
