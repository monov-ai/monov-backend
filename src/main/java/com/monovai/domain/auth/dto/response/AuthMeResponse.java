package com.monovai.domain.auth.dto.response;

import com.monovai.domain.user.entity.User;
import com.monovai.domain.user.entity.enums.Role;

/**
 * GET /auth/me 응답.
 * 미인증 사용자도 호출 가능 — user=null 로 반환.
 */
public record AuthMeResponse(
	UserView user,
	Boolean hasProfile,
	Boolean isAdmin
) {
	public record UserView(
		Long id,
		String email,
		String name,
		String nickname,
		String profileImageUrl
	) {
		public static UserView from(User u) {
			return new UserView(
				u.getId(),
				u.getEmail(),
				u.getName(),
				u.getNickname(),
				u.getProfileImageUrl()
			);
		}
	}

	public static AuthMeResponse anonymous() {
		return new AuthMeResponse(null, false, false);
	}

	public static AuthMeResponse of(User user) {
		boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;
		return new AuthMeResponse(
			UserView.from(user),
			true,
			isAdmin
		);
	}
}