package com.monovai.domain.auth.entity.enums;



import com.monovai.global.error.code.ErrorCode;
import com.monovai.global.error.exception.NotFoundException;

import lombok.Getter;

@Getter
public enum SocialType {
    KAKAO, GOOGLE;

    public static SocialType from(String value) {
        try {
            return SocialType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NotFoundException(ErrorCode.SOCIAL_TYPE_NOT_FOUND);
        }
    }
}
