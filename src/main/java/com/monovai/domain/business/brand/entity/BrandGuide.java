package com.monovai.domain.business.brand.entity;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.monovai.domain.business.brand.entity.value.BrandAsset;
import com.monovai.domain.business.brand.entity.value.BrandGuideline;
import com.monovai.domain.business.brand.entity.value.BrandIdentity;
import com.monovai.domain.business.brand.entity.value.BrandPalette;
import com.monovai.domain.business.brand.entity.value.BrandUsage;
import com.monovai.domain.user.entity.User;
import com.monovai.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "brand_guides")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrandGuide extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false, length = 50)
	private String guideSlug;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false)
	private boolean isDefault;

	@Column(columnDefinition = "TEXT")
	private String description;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private BrandIdentity identity;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private List<BrandPalette> palette = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private List<BrandGuideline> guidelines = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private List<BrandAsset> assets = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private List<BrandUsage> recentUsages = new ArrayList<>();

	@Builder(access = AccessLevel.PRIVATE)
	private BrandGuide(String guideSlug, User user, String name, boolean isDefault, String description) {
		this.guideSlug = guideSlug;
		this.user = user;
		this.name = name;
		this.isDefault = isDefault;
		this.description = description;
		this.identity = BrandIdentity.empty();
	}

	public static BrandGuide create(String slug, User user, String name, boolean isDefault, String description) {
		return BrandGuide.builder()
			.guideSlug(slug).user(user).name(name).isDefault(isDefault).description(description)
			.build();
	}

	public void rename(String name) {
		if (name != null && !name.isBlank()) this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setIsDefault(boolean v) {
		this.isDefault = v;
	}

	public void setIdentity(BrandIdentity identity) {
		this.identity = identity != null ? identity : BrandIdentity.empty();
	}

	public void setPalette(List<BrandPalette> palette) {
		this.palette = palette != null ? palette : new ArrayList<>();
	}

	public void setGuidelines(List<BrandGuideline> guidelines) {
		this.guidelines = guidelines != null ? guidelines : new ArrayList<>();
	}

	public void setAssets(List<BrandAsset> assets) {
		this.assets = assets != null ? assets : new ArrayList<>();
	}

	public void setRecentUsages(List<BrandUsage> recentUsages) {
		this.recentUsages = recentUsages != null ? recentUsages : new ArrayList<>();
	}
}
