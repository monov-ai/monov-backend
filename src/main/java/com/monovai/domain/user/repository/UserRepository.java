package com.monovai.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.auth.entity.enums.SocialType;
import com.monovai.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findBySocialTypeAndSocialId(SocialType socialType, String socialId);

	boolean existsByNickname(String nickname);
}
