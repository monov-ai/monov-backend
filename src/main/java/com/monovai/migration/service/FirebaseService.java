package com.monovai.migration.service;
/*
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.monovai.migration.dto.TemplateMigrationDto;
import com.monovai.migration.dto.UserMigrationDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FirebaseService {

	public List<UserMigrationDto> getUsers() {

		Firestore db = FirestoreClient.getFirestore();
		List<UserMigrationDto> result = new ArrayList<>();

		try {
			QuerySnapshot snapshot = db.collection("users").get().get();

			for (DocumentSnapshot doc : snapshot.getDocuments()) {

				UserMigrationDto dto = UserMigrationDto.builder()
					.uid(doc.getId())
					.email(doc.getString("email"))
					.name(doc.getString("name"))
					.profileImage(doc.getString("profileImage"))
					.createdAt(parseDate(doc, "createdAt"))
					.updatedAt(parseDate(doc, "updatedAt"))
					.build();

				result.add(dto);
			}

		} catch (Exception e) {
			throw new RuntimeException("Firebase users 조회 실패", e);
		}

		return result;
	}

	public List<TemplateMigrationDto> getTemplates() {

		Firestore db = FirestoreClient.getFirestore();
		List<TemplateMigrationDto> result = new ArrayList<>();

		try {
			ApiFuture<QuerySnapshot> future = db.collection("templates").get();
			List<QueryDocumentSnapshot> documents = future.get().getDocuments();

			for (DocumentSnapshot doc : documents) {

				TemplateMigrationDto dto = TemplateMigrationDto.builder()
					.id(doc.getId())
					.title(doc.getString("title"))
					.shortDescription(doc.getString("shortDescription"))
					.prompt(doc.getString("prompt"))
					.promptGuide(doc.getString("promptGuide"))
					.category(doc.getString("category"))
					.imagePath(doc.getString("imagePath"))
					.imageUrl(doc.getString("imageUrl"))
					.mediaType(doc.getString("mediaType"))
					.model(doc.getString("model"))
					.credit(doc.getLong("credit") != null ? doc.getLong("credit").intValue() : 0)
					.hashtags((List<String>) doc.get("hashtags"))
					.createdAt(parseDate(doc, "createdAt"))
					.updatedAt(parseDate(doc, "updatedAt"))
					.build();

				result.add(dto);
			}

		} catch (Exception e) {
			throw new RuntimeException("Firebase templates 조회 실패", e);
		}

		return result;
	}


	private Long parseDate(DocumentSnapshot doc, String field) {
		Object value = doc.get(field);

		if (value instanceof Timestamp ts) {
			return ts.toDate().getTime();
		} else if (value instanceof Long l) {
			return l;
		}
		return null;
	}
}
*/
