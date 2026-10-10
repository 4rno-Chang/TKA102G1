package com.bistroops.permission.model;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository
        extends JpaRepository<PermissionVO, Integer> {

}