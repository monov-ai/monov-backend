package com.monovai.migration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
public class UserMigrationDto {

	private String uid;          // firebase uid
	private String email;
	private String name;
	private String profileImage;

	private Long createdAt;
	private Long updatedAt;
}
