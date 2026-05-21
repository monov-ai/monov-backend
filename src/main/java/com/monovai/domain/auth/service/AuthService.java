package com.monovai.domain.auth.service;

import java.util.List;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.monovai.domain.auth.dto.request.SignupRequest;
import com.monovai.domain.auth.dto.response.AuthMeResponse;
import com.monovai.domain.auth.dto.response.AuthResponse;
import com.monovai.domain.auth.dto.response.JwtResponse;
import com.monovai.domain.auth.dto.response.OAuthUserInformation;
import com.monovai.domain.auth.dto.response.SignUpResponse;
import com.monovai.domain.auth.dto.response.WithdrawResponse;
import com.monovai.domain.auth.entity.enums.SocialType;
import com.monovai.domain.business.imagejob.entity.enums.JobStatus;
import com.monovai.domain.business.imagejob.repository.ImageJobRepository;
import com.monovai.domain.business.video.entity.enums.VideoStatus;
import com.monovai.domain.business.video.repository.VideoTemplateRepository;
import com.monovai.domain.credit.entity.UsageWallet;
import com.monovai.domain.credit.service.CreditService;
import com.monovai.domain.payment.entity.Subscription;
import com.monovai.domain.payment.entity.enums.SubscriptionStatus;
import com.monovai.domain.payment.repository.SubscriptionRepository;
import com.monovai.domain.user.entity.User;
import com.monovai.domain.user.entity.enums.Role;
import com.monovai.domain.user.repository.UserRepository;
import com.monovai.domain.user.service.UserService;
import com.monovai.global.jwt.service.JwtService;
import com.monovai.global.jwt.service.TokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthService {

    private final KakaoService kakaoService;
    private final GoogleService googleService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserService userService;
    private final TokenService tokenService;
    private final CreditService creditService;
    private final ImageJobRepository imageJobRepository;
    private final VideoTemplateRepository videoTemplateRepository;
    private final SubscriptionRepository subscriptionRepository;


	public AuthResponse login(OAuthUserInformation userInfo) {
        return userRepository.findBySocialTypeAndSocialId(userInfo.socialType(), userInfo.socialId())
                .map(this::handleExistingUser)
                .orElseGet(() -> handleNewUser(userInfo));
    }

    private AuthResponse handleNewUser(OAuthUserInformation userInfo) {
        String preSignupToken = jwtService.generatePreSignupToken(userInfo.socialId());
        return AuthResponse.ofNotRegisteredUser(preSignupToken, userInfo);
    }

    private AuthResponse handleExistingUser(final User user) {
        JwtResponse jwtResponse = jwtService.issueToken(user.getId(), user.getRole().name());
        tokenService.saveRefreshToken(user.getId(), jwtResponse.refreshToken());
        return AuthResponse.ofRegisteredUser(user.getId(), user.getNickname(), jwtResponse);
    }

    @Transactional
    public SignUpResponse register(final String authorization, SignupRequest request) {
        log.info(authorization);
        jwtService.validatePreSignupToken(authorization);

        User newUser = convertDtoToEntity(request);

        userRepository.save(newUser);

        JwtResponse token = jwtService.issueToken(newUser.getId(), newUser.getRole().name());
        tokenService.saveRefreshToken(newUser.getId(), token.refreshToken());

        return SignUpResponse.of(
                newUser,
                token
        );
    }

    public JwtResponse reIssueToken(String authorization) {
        return jwtService.reissueToken(authorization);
    }

    public SocialService getSocialServiceByType(SocialType socialType) {
        return switch (socialType) {
            case KAKAO -> kakaoService;
            case GOOGLE -> googleService;
        };
    }

    private User convertDtoToEntity(SignupRequest request) {
        return User.builder()
                .email(request.userInformation().email())
                .name(request.userInformation().name())
                .nickname(request.userInformation().name())
                .profileImageUrl(request.userInformation().profileImageUrl())
                .socialType(request.userInformation().socialType())
                .socialId(request.userInformation().socialId())
                .role(Role.ROLE_BUSINESS)   // 데모 단계: 가입 시 비즈니스 권한 부여
                .build();
    }

    @Transactional
    public WithdrawResponse withdraw(final Long userId){
        User user = userService.getUser(userId);

        // 진행 중 잡 카운트 (즉시 삭제 불가 — 워커가 마무리)
        long pendingImages = imageJobRepository.countByUser_IdAndStatusIn(
            userId, List.of(JobStatus.PENDING, JobStatus.RUNNING, JobStatus.PENDING_IMAGES));
        long pendingVideos = videoTemplateRepository.countByUser_IdAndStatusIn(
            userId, List.of(VideoStatus.REQUESTED, VideoStatus.RUNNING));
        int pending = (int) (pendingImages + pendingVideos);

        //토큰 삭제
        tokenService.deleteRefreshToken(userId);

        //사용자 탈퇴
        userRepository.delete(user);

        log.info("탈퇴 완료 userId={} pendingResources={}", userId, pending);
        return WithdrawResponse.of(pending);
    }

    /**
     * 로그아웃 — refresh token 무효화. access token 은 만료될 때까지 유효 (blacklist 없음).
     */
    @Transactional
    public void logout(final Long userId) {
        tokenService.deleteRefreshToken(userId);
        log.info("logout userId={}", userId);
    }

    /**
     * 현재 인증된 사용자 정보. 미인증 시 user=null 응답.
     */
    public AuthMeResponse getMe(final Long userId) {
        if (userId == null) {
            return AuthMeResponse.anonymous();
        }
        return userRepository.findById(userId)
            .map(user -> {
                UsageWallet wallet = creditService.getWallet(userId);
                Object subscription = subscriptionRepository
                    .findFirstByUser_IdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
                    .map(this::subscriptionSummary)
                    .orElse(null);
                return AuthMeResponse.of(user, wallet, subscription);
            })
            .orElseGet(AuthMeResponse::anonymous);
    }

    private java.util.Map<String, Object> subscriptionSummary(Subscription s) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("planId", s.getPlanId());
        m.put("planName", s.getPlanName());
        m.put("status", s.getStatus().getValue());
        m.put("nextBillingDate", s.getNextBillingDate() != null ? s.getNextBillingDate().toString() : null);
        m.put("currentPeriodEnd", s.getCurrentPeriodEnd() != null ? s.getCurrentPeriodEnd().toString() : null);
        return m;
    }

}
