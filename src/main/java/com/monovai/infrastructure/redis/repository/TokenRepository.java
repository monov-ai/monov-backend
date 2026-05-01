package com.monovai.infrastructure.redis.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.monovai.infrastructure.redis.entity.Token;

@Repository
public interface TokenRepository extends CrudRepository<Token, Long> {
    Optional<Token> findByRefreshToken(String refreshToken);
}
