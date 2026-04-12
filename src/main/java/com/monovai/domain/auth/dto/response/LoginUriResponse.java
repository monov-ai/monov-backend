package com.monovai.domain.auth.dto.response;

public record LoginUriResponse(String uri) {

    public static LoginUriResponse of(String uri) {
        return new LoginUriResponse(uri);
    }
}
