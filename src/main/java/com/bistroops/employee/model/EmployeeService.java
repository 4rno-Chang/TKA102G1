package com.bistroops.employee.model;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

import com.bistroops.permission.model.PermissionVO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.bistroops.permission.model.PermissionRepository;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeePermissionRepository employeePermissionRepository;
    private final PermissionRepository permissionRepository;

    // 建構子注入 Repository
    public EmployeeService(EmployeeRepository employeeRepository, EmployeePermissionRepository employeePermissionRepository ,PermissionRepository permissionRepository) {
        this.employeeRepository = employeeRepository;
        this.employeePermissionRepository = employeePermissionRepository;
        this.permissionRepository = permissionRepository;
    }

    //  新增員工
    @Transactional
    public EmployeeVO addEmployee(EmployeeVO employee) {

        // 1. 檢查手機號碼
        String phone = employee.getEmpTel();

        if (phone == null || !phone.matches("^09\\d{8}$")) {
            throw new IllegalArgumentException("請輸入正確的10碼手機號碼");
        }

        // 2. 新增員工，讓 MySQL 自動產生員工編號
        employee.setEmpNo(null);

        // 3. 設定預設密碼為手機號碼（BCrypt 雜湊）
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        employee.setEmpPassword(encoder.encode(phone));

        // 4. 新增員工預設為在職
        employee.setEmpStatus("1");

        // 5. 儲存員工
        return employeeRepository.save(employee);
    }
    //  查詢所有員工
    public List<EmployeeVO> getAllEmployees() {
        return employeeRepository.findAll();
    }

    //  根據員工編號查詢
    public Optional<EmployeeVO> getEmployeeById(Integer empNo) {
        return employeeRepository.findById(empNo);
    }
    
    // 查詢指定員工擁有的權限編號
    public List<Integer> getEmployeePermissions(Integer empNo) {

        return employeePermissionRepository
                .findByEmployee_EmpNo(empNo)
                .stream()
                .map(ep -> ep.getIds().getPermNo())
                .toList();
    }
    
    
    // 修改指定員工的權限
    @Transactional
    public void updateEmployeePermissions(
            Integer empNo, List<Integer> permNos) {

        // 1. 確認員工存在
        EmployeeVO employee = employeeRepository.findById(empNo)
                .orElseThrow(() ->
                    new IllegalArgumentException("找不到員工：" + empNo));

        // 2. 確認傳入的權限清單有效
        if (permNos == null || permNos.contains(null)) {
            throw new IllegalArgumentException("權限資料不可為 null");
        }

        Set<Integer> uniquePermNos = new HashSet<>(permNos);

        // 3. 確認所有權限編號都存在
        List<PermissionVO> permissions =
                permissionRepository.findAllById(uniquePermNos);

        if (permissions.size() != uniquePermNos.size()) {
            throw new IllegalArgumentException("包含不存在的權限編號");
        }

     // 4. 取得員工目前的權限集合
        Set<EmployeePermissionVO> employeePermissions =
                employee.getEmployeepermissions();

        // 5. 移除這次沒有勾選的權限
        employeePermissions.removeIf(ep ->
                !uniquePermNos.contains(ep.getIds().getPermNo()));

        // 6. 新增原本沒有的權限
        for (PermissionVO permission : permissions) {

            Integer permNo = permission.getPermNo();

            boolean exists = employeePermissions.stream()
                    .anyMatch(ep ->
                            ep.getIds().getPermNo().equals(permNo));

            if (!exists) {
                EmployeePermissionVO ep = new EmployeePermissionVO();

                ep.setIds(new EmployeePermissionId(empNo, permNo));
                ep.setEmployee(employee);
                ep.setPermission(permission);

                employeePermissions.add(ep);
            }
        }

        // 7. 儲存員工權限
        employeeRepository.save(employee);
    }

    //  修改員工資料
    @Transactional
    public EmployeeVO updateEmployee(Integer empNo, EmployeeVO data) {

        EmployeeVO employee = employeeRepository.findById(empNo)
                .orElseThrow(() ->
                        new IllegalArgumentException("找不到員工：" + empNo));

        employee.setEmpName(data.getEmpName());
        employee.setEmpTel(data.getEmpTel());
        employee.setEmpIce(data.getEmpIce());
        employee.setEmpIcetel(data.getEmpIcetel());
        employee.setEmpAdd(data.getEmpAdd());
        employee.setEmpSal(data.getEmpSal());

        return employeeRepository.save(employee);
    }

    //  停用員工
    @Transactional
    public void disableEmployee(Integer empNo) {

        EmployeeVO employee = employeeRepository.findById(empNo)
                .orElseThrow(() ->
                        new IllegalArgumentException("找不到員工：" + empNo));

        employee.setEmpStatus("2");

        employeeRepository.save(employee);
    }
}