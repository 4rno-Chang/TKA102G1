package com.bistroops.employee.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.bistroops.employee.model.EmployeeService;
import com.bistroops.employee.model.EmployeeVO;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // 查詢全部員工
    @GetMapping
    public List<EmployeeVO> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    // 根據員工編號查詢
    @GetMapping("/{empNo}")
    public ResponseEntity<EmployeeVO> getEmployeeById(
            @PathVariable Integer empNo) {

        return employeeService.getEmployeeById(empNo)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    // 查詢指定員工擁有的權限
    @GetMapping("/{empNo}/permissions")
    public ResponseEntity<List<Integer>> getEmployeePermissions(
            @PathVariable Integer empNo) {

        // 確認員工是否存在
        if (employeeService.getEmployeeById(empNo).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 查詢員工權限
        List<Integer> permissions = employeeService.getEmployeePermissions(empNo);

        return ResponseEntity.ok(permissions);
    }
    
    // 修改指定員工的權限
    @PutMapping("/{empNo}/permissions")
    public ResponseEntity<String> updateEmployeePermissions(
            @PathVariable Integer empNo,
            @RequestBody List<Integer> permNos) {

        try {
            employeeService.updateEmployeePermissions(empNo, permNos);

            return ResponseEntity.ok("員工權限修改成功");

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    // 新增員工
    @PostMapping
    public ResponseEntity<Integer> addEmployee(
            @RequestBody EmployeeVO employee) {

        // 檢查姓名
        if (employee.getEmpName() == null
                || employee.getEmpName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "員工姓名不可空白");
        }

        try {
            // 新增員工
            EmployeeVO savedEmployee = employeeService.addEmployee(employee);

            // 回傳 MySQL 自動產生的員工編號
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(savedEmployee.getEmpNo());

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
    
    // 修改員工資料
    @PutMapping("/{empNo}")
    public ResponseEntity<Void> updateEmployee(
            @PathVariable Integer empNo,
            @RequestBody EmployeeVO employee) {

        if (employeeService.getEmployeeById(empNo).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        employeeService.updateEmployee(empNo, employee);

        return ResponseEntity.noContent().build();
    }
    
    // 停用員工（設定為離職）
    @PatchMapping("/{empNo}/disable")
    public ResponseEntity<Void> disableEmployee(
            @PathVariable Integer empNo) {

        if (employeeService.getEmployeeById(empNo).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        employeeService.disableEmployee(empNo);

        return ResponseEntity.noContent().build();
    }
}