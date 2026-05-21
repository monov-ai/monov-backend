package com.monovai.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.monovai.domain.credit.entity.UsageWallet;
import com.monovai.domain.user.entity.User;
import com.monovai.domain.user.entity.enums.Role;

/**
 * GET /auth/me 응답. 미인증 사용자도 호출 가능 — user=null.
 * v2.0: isBusiness / credits / usage / consents / subscription 보강.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthMeResponse(
	UserView user,
	Boolean hasProfile,
	Boolean isAdmin,
	Boolean isBusiness,
	Integer credits,
	Usage usage,
	Object subscription,
	Consents consents
) {
	public record UserView(
		Long id,
		String email,
		String name,
		String nickname,
		String profileImageUrl
	) {
		public static UserView from(User u) {
			return new UserView(u.getId(), u.getEmail(), u.getName(), u.getNickname(), u.getProfileImageUrl());
		}
	}

	public record Usage(int adImage, int aiVideo, int imageToVideo, int legacyCredits) {
	}

	public record Consents(
		Boolean termsAccepted,
		Boolean privacyAccepted,
		Boolean contentUsageAccepted,
		Boolean marketingAccepted,
		String version
	) {
	}

	public static AuthMeResponse anonymous() {
		return new AuthMeResponse(null, false, false, false, null, null, null, null);
	}

	public static AuthMeResponse of(User user, UsageWallet wallet, Object subscription) {
		boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;
		Usage usage = wallet != null
			? new Usage(wallet.getAdImage(), wallet.getAiVideo(), wallet.getImageToVideo(), wallet.getLegacyCredits())
			: new Usage(0, 0, 0, 0);
		int credits = wallet != null ? wallet.getLegacyCredits() : 0;
		Consents consents = user.getTermsAccepted() != null
			? new Consents(user.getTermsAccepted(), user.getPrivacyAccepted(),
				user.getContentUsageAccepted(), user.getMarketingAccepted(), user.getConsentVersion())
			: null;
		return new AuthMeResponse(
			UserView.from(user),
			true,
			isAdmin,
			true,            // 데모 단계: 모든 가입자에게 비즈니스 권한 개방
			credits,
			usage,
			subscription,
			consents
		);
	}
}
