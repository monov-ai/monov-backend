package com.monovai.migration.dto.converter;

import java.time.ZoneId;
import java.util.Map;
/*

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.monovai.domain.user.entity.UserOnboarding;

public class UserOnboardingConverter {

	public static UserOnboarding toOnboarding(DocumentSnapshot doc, Long userId) {

		Map<String, Object> onboarding = doc.get("onboarding", Map.class);

		if (onboarding == null) return null;

		return UserOnboarding.builder()
			.userId(userId)
			.job((String) onboarding.get("job"))
			.source((String) onboarding.get("source"))
			.completedAt(
				((Timestamp) onboarding.get("completedAt"))
					.toDate().toInstant()
					.atZone(ZoneId.systemDefault())
					.toLocalDateTime()
			)
			.build();
	}
}
*/
