package com.monovai.domain.business.brand.entity.value;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BrandGuideline(String key, String title, String summary, String body) {
}
