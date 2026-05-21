package com.monovai.domain.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.auth.entity.enums.SocialType;
import com.monovai.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findBySocialTypeAndSocialId(SocialType socialType, String socialId);

	boolean existsByNickname(String nickname);

	List<User> findByEmailContainingIgnoreCaseOrNicknameContainingIgnoreCase(
		String email, String nickname, Pageable pageable);

	long countByCreatedAtAfter(java.time.LocalDateTime since);
}
