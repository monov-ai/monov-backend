package com.monovai.migration.runner;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
/*
import com.monovai.migration.dto.TemplateMigrationDto;
import com.monovai.migration.dto.UserMigrationDto;
import com.monovai.migration.service.FirebaseService;
import com.monovai.migration.service.TemplateMigrationService;
import com.monovai.migration.service.UserMigrationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MigrationRunner implements CommandLineRunner {

	private final FirebaseService firebaseService;
	private final TemplateMigrationService templateMigrationService;
	private final UserMigrationService userMigrationService;

	@Override
	public void run(String... args) {

		// 1. Firebase에서 데이터 가져오기
		List<UserMigrationDto> users = firebaseService.getUsers();
		List<TemplateMigrationDto> templates = firebaseService.getTemplates();

		System.out.println("🔥 Users count: " + users.size());
		System.out.println("🔥 Templates count: " + templates.size());

		// 2. User 마이그레이션
		users.forEach(userMigrationService::migrateUser);

		// 3. Template 마이그레이션
		templates.forEach(templateMigrationService::migrate);

		System.out.println("✅ Migration 완료");
	}
}
*/
