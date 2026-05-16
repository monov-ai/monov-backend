package com.monovai.domain.business.brand.entity.value;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BrandUsage(String id, String label, String at) {
}
