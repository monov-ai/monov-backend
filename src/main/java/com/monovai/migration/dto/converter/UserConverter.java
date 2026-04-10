package com.monovai.migration.dto.converter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.monovai.domain.user.entity.User;

import lombok.Builder;

@Component
public class UserConverter {

	public User from(DocumentSnapshot doc) {

		Map<String, Object> onboarding = (Map<String, Object>) doc.get("onboarding");

		return User.builder()
			.uid(doc.getString("uid"))
			.email(doc.getString("email"))
			.name(doc.getString("name"))
			.picture(doc.getString("picture"))
			.provider(doc.getString("provider"))
			.credits(getInteger(doc.getLong("credits")))
			.job(onboarding != null ? (String) onboarding.get("job") : null)
			.source(onboarding != null ? (String) onboarding.get("source") : null)
			.onboardingCompletedAt(
				onboarding != null ? toLocalDateTime((Timestamp) onboarding.get("completedAt")) : null
			)
			.createdAt(toLocalDateTime(doc.getTimestamp("createdAt")))
			.updatedAt(toLocalDateTime(doc.getTimestamp("updatedAt")))
			.build();
	}

	private Integer getInteger(Long value) {
		return value != null ? value.intValue() : null;
	}

	private LocalDateTime toLocalDateTime(Timestamp timestamp) {
		if (timestamp == null) return null;
		return timestamp.toDate()
			.toInstant()
			.atZone(ZoneId.systemDefault())
			.toLocalDateTime();
	}
}
