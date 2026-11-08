package com.allvote.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.allvote.entities.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);
}
