package com.allvote.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.allvote.entities.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByNomRole(String nomRole);
}
