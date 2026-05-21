package com.monovai.domain.user.dto.response;

import java.time.Instant;
import java.time.ZoneOffset;

import com.monovai.domain.user.entity.User;

public record OnboardingResponse(
	boolean ok,
	boolean completed,
	Onboarding onboarding,
	Instant completedAt
) {
	public record Onboarding(String job, String source, Instant completedAt) {
	}

	public static OnboardingResponse posted(User u) {
		Instant at = u.getOnboardingCompletedAt() != null
			? u.getOnboardingCompletedAt().toInstant(ZoneOffset.UTC) : null;
		return new OnboardingResponse(true, u.isOnboardingCompleted(), null, at);
	}

	public static OnboardingResponse fetched(User u) {
		Instant at = u.getOnboardingCompletedAt() != null
			? u.getOnboardingCompletedAt().toInstant(ZoneOffset.UTC) : null;
		Onboarding onboarding = u.isOnboardingCompleted()
			? new Onboarding(u.getOnboardingJob(), u.getOnboardingSource(), at) : null;
		return new OnboardingResponse(true, u.isOnboardingCompleted(), onboarding, at);
	}
}
