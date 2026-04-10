package com.monovai.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.monovai.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
