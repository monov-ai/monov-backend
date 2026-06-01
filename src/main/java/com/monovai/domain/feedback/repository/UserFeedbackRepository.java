package com.monovai.domain.feedback.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.feedback.entity.UserFeedback;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {
	List<UserFeedback> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
