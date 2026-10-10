package com.bistroops.permission.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bistroops.permission.model.PermissionRepository;
import com.bistroops.permission.model.PermissionVO;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionRepository permissionRepository;

    // 建構子注入 Repository
    public PermissionController(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    // 查詢所有權限
    @GetMapping
    public List<PermissionVO> getAllPermissions() {
        return permissionRepository.findAll();
    }
}