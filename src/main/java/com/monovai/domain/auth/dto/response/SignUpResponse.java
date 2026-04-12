package com.monovai.domain.auth.dto.response;

import java.util.List;

import com.monovai.domain.user.entity.User;

public record SignUpResponse(
        Long userId,

        JwtResponse jwtResponse
) {

    public static SignUpResponse of(User user, JwtResponse jwtResponse) {
        return new SignUpResponse(
                user.getId(),
                jwtResponse
        );
    }
}
