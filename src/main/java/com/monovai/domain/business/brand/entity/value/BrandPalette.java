package com.monovai.domain.business.brand.entity.value;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BrandPalette(String role, String label, String hex) {
}
