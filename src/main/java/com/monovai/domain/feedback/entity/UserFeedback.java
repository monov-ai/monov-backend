package com.monovai.domain.feedback.entity;

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

/**
 * §5: 사용자가 결과물을 다운로드한 뒤 남기는 별점/태그/메모.
 */
@Entity
@Table(name = "user_feedbacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFeedback extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/** "business_template" | "studio_template" | 그 외 free-form. */
	@Column(length = 40)
	private String type;

	/** "video" | "image". */
	@Column(length = 20)
	private String mediaType;

	@Column(length = 100)
	private String templateId;

	@Column(length = 200)
	private String templateTitle;

	@Column(length = 100)
	private String itemId;

	@Column(length = 20)
	private String downloadSource;

	@Column(nullable = false)
	private int rating;

	@Column(length = 100)
	private String tag;

	@Column(length = 300)
	private String note;

	@Builder(access = AccessLevel.PRIVATE)
	private UserFeedback(User user, String type, String mediaType, String templateId,
		String templateTitle, String itemId, String downloadSource, int rating, String tag, String note) {
		this.user = user;
		this.type = type;
		this.mediaType = mediaType;
		this.templateId = templateId;
		this.templateTitle = templateTitle;
		this.itemId = itemId;
		this.downloadSource = downloadSource;
		this.rating = rating;
		this.tag = tag;
		this.note = note;
	}

	public static UserFeedback of(User user, String type, String mediaType, String templateId,
		String templateTitle, String itemId, String downloadSource, int rating, String tag, String note) {
		return UserFeedback.builder()
			.user(user).type(type).mediaType(mediaType).templateId(templateId)
			.templateTitle(templateTitle).itemId(itemId).downloadSource(downloadSource)
			.rating(rating).tag(tag).note(note)
			.build();
	}
}
