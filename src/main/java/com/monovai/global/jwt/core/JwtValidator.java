package com.monovai.global.jwt.core;

import java.util.Date;


import org.springframework.stereotype.Component;

import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.UnauthorizedException;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtValidator {
    private final JwtExtractor jwtExtractor;

    public boolean isExpired(String token) {
        Claims claims = jwtExtractor.extractClaims(token);
        return claims.getExpiration().before(new Date());
    }

    public boolean isIssuedInFuture(String token) {
        Claims claims = jwtExtractor.extractClaims(token);
        return claims.getIssuedAt().after(new Date());
    }

    public boolean hasUserId(String token) {
        Claims claims = jwtExtractor.extractClaims(token);
        return claims.get("userId") != null;
    }

    public boolean hasSocialId(String token){
        Claims claims = jwtExtractor.extractClaims(token);
        return claims.get("socialId") != null;
    }

    public void validatePreSignupToken(String token) {
        if (isExpired(token) || !hasSocialId(token)) {
            throw new UnauthorizedException();
        }
    }

    public void validateRefreshToken(String token) {
        if (isExpired(token) || !hasUserId(token)) {
            throw new UnauthorizedException(ErrorCode.REFRESH_TOKEN_EXPIRED_ERROR);
        }
    }
}
