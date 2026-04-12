package com.monovai.global.jwt.core;

import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.monovai.global.jwt.config.JwtProperties;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtProvider {

    private static final String USER_ID = "userId";
    private static final String ROLE = "role";
    private static final String SOCIAL_ID = "socialId";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public String generateAccessToken(Long userId, String role) {
        return generateToken(Map.of(USER_ID, userId, ROLE, role), jwtProperties.getAccessTokenExpirationTime());
    }

    public String generateRefreshToken(Long userId, String role) {
        return generateToken(Map.of(USER_ID, userId, ROLE, role), jwtProperties.getRefreshTokenExpirationTime());
    }

    public String generatePreSignupToken(String socialId){
        return generateToken(Map.of(SOCIAL_ID, socialId), jwtProperties.getPreSignupTokenExpirationTime());
    }

    public String generateToken(Map<String, Object> claims, long expirationTime) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationTime);
        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }
}
