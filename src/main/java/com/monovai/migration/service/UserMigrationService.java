package com.monovai.migration.service;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;

import com.monovai.domain.user.entity.User;
import com.monovai.domain.user.repository.UserRepository;
import com.monovai.migration.dto.UserMigrationDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserMigrationService {

	private final UserRepository userRepository;

	public void migrateUser(UserMigrationDto dto) {

		User user = User.builder()
			.uid(dto.getUid())
			.email(dto.getEmail())
			.name(dto.getName())
			.picture(dto.getProfileImage())
			.credits(0)
			.createdAt(toLocalDateTime(dto.getCreatedAt()))
			.updatedAt(toLocalDateTime(dto.getUpdatedAt()))
			.build();

		userRepository.save(user);
	}

	private LocalDateTime toLocalDateTime(Long millis) {
		if (millis == null) return null;
		return Instant.ofEpochMilli(millis)
			.atZone(ZoneId.systemDefault())
			.toLocalDateTime();
	}
}
