package com.monovai.domain.business.imagejob.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.business.imagejob.entity.ImageJob;

public interface ImageJobRepository extends JpaRepository<ImageJob, Long> {
}