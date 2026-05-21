package com.monovai.domain.payment;

import java.util.Map;
import java.util.Optional;

/**
 * 패키지(단건) / 플랜(구독) 카탈로그. 가격(원) + 충전 크레딧/월정액 매핑.
 */
public final class PaymentCatalog {

	private PaymentCatalog() {
	}

	public record Package(String id, int amount, int credits) {
	}

	public record Plan(String id, String name, int amount,
		int legacyCredits, int adImage, int aiVideo, int imageToVideo) {
	}

	private static final Map<String, Package> PACKAGES = Map.of(
		"credit_pack_starter", new Package("credit_pack_starter", 9900, 200),
		"credit_pack_basic", new Package("credit_pack_basic", 19900, 450),
		"credit_pack_pro", new Package("credit_pack_pro", 49900, 1300)
	);

	private static final Map<String, Plan> PLANS = Map.of(
		"pro_monthly", new Plan("pro_monthly", "Pro 월간", 29900, 1600, 0, 0, 0),
		"business_monthly", new Plan("business_monthly", "Business 월간", 79900, 4800, 0, 0, 0)
	);

	public static Optional<Package> findPackage(String id) {
		return Optional.ofNullable(PACKAGES.get(id));
	}

	public static Optional<Plan> findPlan(String id) {
		return Optional.ofNullable(PLANS.get(id));
	}
}
