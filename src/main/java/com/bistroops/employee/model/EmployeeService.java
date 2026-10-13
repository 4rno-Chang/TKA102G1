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
	public EmployeeService(EmployeeRepository employeeRepository,
			EmployeePermissionRepository employeePermissionRepository, PermissionRepository permissionRepository) {
		this.employeeRepository = employeeRepository;
		this.employeePermissionRepository = employeePermissionRepository;
		this.permissionRepository = permissionRepository;
	}

	// 新增員工
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

		// 新增員工尚未完成首次密碼修改
		employee.setEmpPwdChanged(false);

		// 5. 儲存員工
		return employeeRepository.save(employee);
	}

	// 員工登入驗證
	@Transactional(readOnly = true)
	public EmployeeVO login(Integer empNo, String password) {

		// 1. 根據員工編號查詢
		EmployeeVO employee = employeeRepository.findById(empNo).orElse(null);

		// 2. 員工不存在
		if (employee == null) {
			return null;
		}

		// 3. 停用員工不可登入
		if (!"1".equals(employee.getEmpStatus())) {
			return null;
		}

		// 4. 驗證員工密碼（BCrypt），並相容資料庫中帶有 {bcrypt} 前綴的密碼
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

		String storedPassword = employee.getEmpPassword();

		// 資料庫沒有密碼，登入失敗
		if (storedPassword == null) {
			return null;
		}

		// 移除 {bcrypt} 前綴，讓 BCryptPasswordEncoder 可以正常驗證
		if (storedPassword.startsWith("{bcrypt}")) {
			storedPassword = storedPassword.substring("{bcrypt}".length());
		}

		// 比對使用者輸入的密碼與資料庫中的密碼
		if (password == null || !encoder.matches(password, storedPassword)) {
			return null;
		}

		// 5. 驗證成功，回傳員工資料
		return employee;
	}

	// 員工修改密碼
	@Transactional
	public void changePassword(Integer empNo, String oldPassword, String newPassword) {

		// 1. 查詢員工
		EmployeeVO employee = employeeRepository.findById(empNo)
				.orElseThrow(() -> new IllegalArgumentException("找不到員工"));

		// 2. 確認員工仍在職
		if (!"1".equals(employee.getEmpStatus())) {
			throw new IllegalArgumentException("此員工已停用");
		}

		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

		// 3. 驗證舊密碼，並相容 {bcrypt} 前綴
		String storedPassword = employee.getEmpPassword();

		if (storedPassword == null) {
			throw new IllegalArgumentException("原密碼錯誤");
		}

		if (storedPassword.startsWith("{bcrypt}")) {
			storedPassword = storedPassword.substring("{bcrypt}".length());
		}

		if (oldPassword == null || !encoder.matches(oldPassword, storedPassword)) {
			throw new IllegalArgumentException("原密碼錯誤");
		}

		// 4. 檢查新密碼：至少8碼，必須包含英文與數字
		if (newPassword == null || !newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$")) {
			throw new IllegalArgumentException("新密碼至少8碼，且必須包含英文及數字");
		}

		// 5. 新密碼不可與舊密碼相同
		if (encoder.matches(newPassword, storedPassword)) {
			throw new IllegalArgumentException("新密碼不可與原密碼相同");
		}

		// 6. 將新密碼加密後儲存
		employee.setEmpPassword(encoder.encode(newPassword));

		// 7. 標記已完成首次密碼修改
		employee.setEmpPwdChanged(true);

		// 8. 更新資料庫
		employeeRepository.save(employee);
	}

	// 查詢所有員工
	public List<EmployeeVO> getAllEmployees() {
		return employeeRepository.findAll();
	}

	// 根據員工編號查詢
	public Optional<EmployeeVO> getEmployeeById(Integer empNo) {
		return employeeRepository.findById(empNo);
	}

	// 查詢指定員工擁有的權限編號
	public List<Integer> getEmployeePermissions(Integer empNo) {

		return employeePermissionRepository.findByEmployee_EmpNo(empNo).stream().map(ep -> ep.getIds().getPermNo())
				.toList();
	}

	// 修改指定員工的權限
	@Transactional
	public void updateEmployeePermissions(Integer empNo, List<Integer> permNos) {

		// 1. 確認員工存在
		EmployeeVO employee = employeeRepository.findById(empNo)
				.orElseThrow(() -> new IllegalArgumentException("找不到員工：" + empNo));

		// 停用員工不可修改權限
		if ("2".equals(employee.getEmpStatus())) {
			throw new IllegalArgumentException("此員工已停用，無法修改權限");
		}

		// 2. 確認傳入的權限清單有效
		if (permNos == null || permNos.contains(null)) {
			throw new IllegalArgumentException("權限資料不可為 null");
		}

		Set<Integer> uniquePermNos = new HashSet<>(permNos);

		// 3. 確認所有權限編號都存在
		List<PermissionVO> permissions = permissionRepository.findAllById(uniquePermNos);

		if (permissions.size() != uniquePermNos.size()) {
			throw new IllegalArgumentException("包含不存在的權限編號");
		}

		// 4. 取得員工目前的權限集合
		Set<EmployeePermissionVO> employeePermissions = employee.getEmployeepermissions();

		// 5. 移除這次沒有勾選的權限
		employeePermissions.removeIf(ep -> !uniquePermNos.contains(ep.getIds().getPermNo()));

		// 6. 新增原本沒有的權限
		for (PermissionVO permission : permissions) {

			Integer permNo = permission.getPermNo();

			boolean exists = employeePermissions.stream().anyMatch(ep -> ep.getIds().getPermNo().equals(permNo));

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

	// 修改員工資料
	@Transactional
	public EmployeeVO updateEmployee(Integer empNo, EmployeeVO data) {

		EmployeeVO employee = employeeRepository.findById(empNo)
				.orElseThrow(() -> new IllegalArgumentException("找不到員工：" + empNo));

		// 停用員工不可修改資料
		if ("2".equals(employee.getEmpStatus())) {
			throw new IllegalArgumentException("此員工已停用，無法修改資料");
		}

		employee.setEmpName(data.getEmpName());
		employee.setEmpTel(data.getEmpTel());
		employee.setEmpIce(data.getEmpIce());
		employee.setEmpIcetel(data.getEmpIcetel());
		employee.setEmpAdd(data.getEmpAdd());
		employee.setEmpSal(data.getEmpSal());

		return employeeRepository.save(employee);
	}

	// 停用員工
	@Transactional
	public void disableEmployee(Integer empNo) {

		EmployeeVO employee = employeeRepository.findById(empNo)
				.orElseThrow(() -> new IllegalArgumentException("找不到員工：" + empNo));

		employee.setEmpStatus("2");

		employeeRepository.save(employee);
	}

	// 取消停用員工（恢復在職）
	@Transactional
	public void enableEmployee(Integer empNo) {

		EmployeeVO employee = employeeRepository.findById(empNo)
				.orElseThrow(() -> new IllegalArgumentException("找不到員工：" + empNo));

		// 將員工狀態恢復為在職
		employee.setEmpStatus("1");

		employeeRepository.save(employee);
	}

	// 上傳或更換員工照片
	@Transactional
	public void updateEmployeePhoto(Integer empNo, byte[] photo) {

		EmployeeVO employee = employeeRepository.findById(empNo)
				.orElseThrow(() -> new IllegalArgumentException("找不到員工：" + empNo));

		// 停用員工不可更換照片
		if ("2".equals(employee.getEmpStatus())) {
			throw new IllegalArgumentException("此員工已停用，無法修改照片");
		}

		employee.setEmpPhoto(photo);

		employeeRepository.save(employee);
	}

	// 查詢員工照片
	@Transactional(readOnly = true)
	public byte[] getEmployeePhoto(Integer empNo) {

		EmployeeVO employee = employeeRepository.findById(empNo)
				.orElseThrow(() -> new IllegalArgumentException("找不到員工：" + empNo));

		return employee.getEmpPhoto();
	}

	// 刪除員工照片
	@Transactional
	public void deleteEmployeePhoto(Integer empNo) {

		// 1. 確認員工存在
		EmployeeVO employee = employeeRepository.findById(empNo)
				.orElseThrow(() -> new IllegalArgumentException("找不到員工：" + empNo));

		// 2. 停用員工不可修改照片
		if ("2".equals(employee.getEmpStatus())) {
			throw new IllegalArgumentException("此員工已停用，無法刪除照片");
		}

		// 3. 清除員工照片
		employee.setEmpPhoto(null);

		// 4. 更新資料庫
		employeeRepository.save(employee);
	}

}