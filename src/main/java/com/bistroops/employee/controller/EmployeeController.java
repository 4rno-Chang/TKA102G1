package com.bistroops.employee.controller;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.bistroops.config.EmployeeAuthenticationService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;

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
	private final com.bistroops.employee.model.EmployeeAccountService employeeAccountService;
	private final EmployeeAuthenticationService employeeAuthenticationService;

	public EmployeeController(
	        EmployeeService employeeService,
	        EmployeeAuthenticationService employeeAuthenticationService,
	        com.bistroops.employee.model.EmployeeAccountService employeeAccountService) {

	    this.employeeService = employeeService;
	    this.employeeAccountService = employeeAccountService;
	    this.employeeAuthenticationService = employeeAuthenticationService;
	}

	// 員工登入
	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(
	        @RequestBody Map<String, String> data,
	        HttpSession session) {

	    // 1. 取得員工編號與密碼
	    String empNoText = data.get("empNo");
	    String password = data.get("password");

	    if (empNoText == null || !empNoText.matches("\\d+")
	            || password == null || password.isBlank()) {

	        return ResponseEntity.badRequest()
	                .body(Map.of("message", "請輸入正確的員工編號及密碼"));
	    }

	    Integer empNo;

	    try {
	        empNo = Integer.valueOf(empNoText);
	    } catch (NumberFormatException e) {
	        return ResponseEntity.badRequest()
	                .body(Map.of("message", "員工編號格式錯誤"));
	    }

	    // 2. 使用 Spring Security 員工認證服務驗證
	    Authentication authentication =
	            employeeAuthenticationService.authenticate(empNo, password);

	    if (authentication == null) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                .body(Map.of("message", "員工編號或密碼錯誤、帳號已停用，或臨時密碼已過期；請聯絡主管協助"));
	    }

	    // 3. 取得員工資料
	    EmployeeVO employee = employeeService.getEmployeeById(empNo)
	            .orElseThrow();

	    // 4. 建立 Spring Security 認證資訊
	    SecurityContext context = SecurityContextHolder.createEmptyContext();
	    context.setAuthentication(authentication);

	    SecurityContextHolder.setContext(context);

	    // 5. 將 Spring Security 認證資訊存入 Session
	    session.setAttribute(
	            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
	            context
	    );

	    // 6. 保留原本的員工 Session 與 12 小時登入期限
	    session.setAttribute("employeeNo", employee.getEmpNo());

	    session.setMaxInactiveInterval(12 * 60 * 60);

	 // 員工登入後固定 12 小時到期
	    session.setAttribute(
	        "employeeExpireAt",
	        System.currentTimeMillis() + 12 * 60 * 60 * 1000L
	    );

	    // 7. 回傳原本的登入結果
	    return ResponseEntity.ok(Map.of(
	            "message", "登入成功",
	            "empNo", employee.getEmpNo(),
	            "empName", employee.getEmpName(),
	            "mustChangePassword",
	            !Boolean.TRUE.equals(employee.getEmpPwdChanged())
	    ));
	}

	// 只有後臺管理權限的主管可以重設員工密碼
	@PostMapping("/{empNo}/reset-password")
	public ResponseEntity<?> resetPassword(
	        @PathVariable Integer empNo, HttpSession session) {
	    Integer loginEmpNo = (Integer) session.getAttribute("employeeNo");
	    if (loginEmpNo == null) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "請先登入"));
	    }
	    if (!employeeService.hasPermission(loginEmpNo, 7)) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "權限不足"));
	    }
	    if (employeeService.getEmployeeById(empNo).isEmpty()) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "找不到員工"));
	    }
	    try {
	        return ResponseEntity.ok(employeeAccountService.resetPassword(empNo));
	    } catch (IllegalArgumentException e) {
	        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
	    }
	}

	// 員工修改密碼
	@PostMapping("/change-password")
	public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> data,
			HttpSession session) {

		// 1. 確認員工已登入
		Integer empNo = (Integer) session.getAttribute("employeeNo");

		if (empNo == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "請先登入"));
		}

		// 2. 只取得新密碼
		String newPassword = data.get("newPassword");

		// 3. 呼叫 Service 修改密碼
		try {
		    employeeService.changePassword(empNo, newPassword);

			return ResponseEntity.ok(Map.of("message", "密碼修改成功"));

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		}
	}

	// 員工登出
	@PostMapping("/logout")
	public ResponseEntity<Map<String, String>> logout(HttpSession session) {

	    // 1. 清除 Spring Security 認證資訊
	    SecurityContextHolder.clearContext();

	    session.removeAttribute(
	            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
	    );

	    // 2. 清除原本員工登入資訊
	    session.removeAttribute("employeeNo");
	    session.removeAttribute("employeeExpireAt");

	    // 3. 回傳登出結果
	    return ResponseEntity.ok(Map.of("message", "登出成功"));
	}

	// 員工自行修改個人資料
	@PutMapping("/me")
	public ResponseEntity<Map<String, String>> updateMyProfile(@RequestBody Map<String, String> data,
			HttpSession session) {

		// 1. 從 Session 取得目前登入員工編號
		Integer empNo = (Integer) session.getAttribute("employeeNo");

		if (empNo == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "請先登入"));
		}

		// 2. 只接收允許修改的三個欄位
		String empIce = data.get("empIce");
		String empIcetel = data.get("empIcetel");
		String empAdd = data.get("empAdd");

		// 3. 驗證資料長度
		if ((empIce != null && empIce.length() > 30) || (empIcetel != null && empIcetel.length() > 10)
				|| (empAdd != null && empAdd.length() > 100)) {
			return ResponseEntity.badRequest().body(Map.of("message", "個人資料超過允許長度"));
		}

		// 4. 更新資料庫
		try {
			employeeService.updateMyProfile(empNo, empIce, empIcetel, empAdd);

			return ResponseEntity.ok(Map.of("message", "個人資料修改成功"));

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		}
	}

	// 查詢目前登入員工的個人資料
	@GetMapping("/me")
	public ResponseEntity<Map<String, Object>> getMyProfile(HttpSession session) {

		// 1. 從 Session 取得目前登入的員工編號
		Integer empNo = (Integer) session.getAttribute("employeeNo");

		if (empNo == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		// 2. 查詢目前登入的員工
		EmployeeVO employee = employeeService.getEmployeeById(empNo).orElse(null);

		if (employee == null || !"1".equals(employee.getEmpStatus())) {
			session.removeAttribute("employeeNo");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		// 3. 只回傳個人資料視窗需要的欄位
		Map<String, Object> profile = new java.util.HashMap<>();

		profile.put("empNo", employee.getEmpNo());
		profile.put("empName", employee.getEmpName());
		profile.put("empTel", employee.getEmpTel());
		profile.put("empIce", employee.getEmpIce());
		profile.put("empIcetel", employee.getEmpIcetel());
		profile.put("empAdd", employee.getEmpAdd());
		profile.put("hasPhoto", employee.getEmpPhoto() != null && employee.getEmpPhoto().length > 0);

		return ResponseEntity.ok(profile);
	}

	// 查詢全部員工
	@GetMapping
	public List<EmployeeVO> getAllEmployees() {
	    return employeeService.getAllEmployees();
	}

	// 根據員工編號查詢
	@GetMapping("/{empNo}")
	public ResponseEntity<EmployeeVO> getEmployeeById(@PathVariable Integer empNo) {

		return employeeService.getEmployeeById(empNo).map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	// 查詢指定員工擁有的權限
	// 查詢指定員工擁有的權限
	@GetMapping("/{empNo}/permissions")
	public ResponseEntity<List<Integer>> getEmployeePermissions(
	        @PathVariable Integer empNo,
	        HttpSession session) {

	    // 1. 確認操作者已登入
	    Integer loginEmpNo =
	            (Integer) session.getAttribute("employeeNo");

	    if (loginEmpNo == null) {
	        return ResponseEntity
	                .status(HttpStatus.UNAUTHORIZED)
	                .build();
	    }

	    // 2. 只有權限 7 可以查詢員工權限
	    if (!employeeService.hasPermission(loginEmpNo, 7)) {
	        return ResponseEntity
	                .status(HttpStatus.FORBIDDEN)
	                .build();
	    }

	    // 3. 確認被查詢的員工存在
	    if (employeeService.getEmployeeById(empNo).isEmpty()) {
	        return ResponseEntity.notFound().build();
	    }

	    // 4. 查詢員工權限
	    List<Integer> permissions =
	            employeeService.getEmployeePermissions(empNo);

	    return ResponseEntity.ok(permissions);
	}

	// 修改指定員工的權限
	@PutMapping("/{empNo}/permissions")
	public ResponseEntity<String> updateEmployeePermissions(
	        @PathVariable Integer empNo,
	        @RequestBody List<Integer> permNos,
	        HttpSession session) {

	    // 1. 確認操作者已登入
	    Integer loginEmpNo =
	            (Integer) session.getAttribute("employeeNo");

	    if (loginEmpNo == null) {
	        return ResponseEntity
	                .status(HttpStatus.UNAUTHORIZED)
	                .body("請先登入");
	    }

	    // 2. 確認操作者擁有後臺管理權限（7）
	    if (!employeeService.hasPermission(loginEmpNo, 7)) {
	        return ResponseEntity
	                .status(HttpStatus.FORBIDDEN)
	                .body("權限不足");
	    }

	    if (loginEmpNo.equals(empNo)) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN)
	                .body("無法修改自己的權限");
	    }

	    // 3. 修改指定員工的權限
	    try {
	        employeeService.updateEmployeePermissions(
	                empNo, permNos);

	        return ResponseEntity.ok("員工權限修改成功");

	    } catch (IllegalArgumentException e) {
	        return ResponseEntity
	                .badRequest()
	                .body(e.getMessage());
	    }
	}

	// 新增員工
	@PostMapping
	public ResponseEntity<?> addEmployee(@RequestBody EmployeeVO employee) {

		// 檢查姓名
		if (employee.getEmpName() == null || employee.getEmpName().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "員工姓名不可空白");
		}

		try {
			// 新增員工
			var result = employeeAccountService.createEmployee(employee);

			// 回傳 MySQL 自動產生的員工編號
			return ResponseEntity.status(HttpStatus.CREATED).body(result);

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
		}
	}

	// 修改員工資料
	@PutMapping("/{empNo}")
	public ResponseEntity<Void> updateEmployee(@PathVariable Integer empNo, @RequestBody EmployeeVO employee) {

		if (employeeService.getEmployeeById(empNo).isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		try {
			employeeService.updateEmployee(empNo, employee);

			return ResponseEntity.noContent().build();

		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

	// 停用員工（設定為離職）
	@PatchMapping("/{empNo}/disable")
	public ResponseEntity<Void> disableEmployee(@PathVariable Integer empNo, HttpSession session) {
		Integer loginEmpNo = (Integer) session.getAttribute("employeeNo");
		if (loginEmpNo == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入");
		}
		if (loginEmpNo.equals(empNo)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "無法停用自己的帳號");
		}

		if (employeeService.getEmployeeById(empNo).isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		employeeService.disableEmployee(empNo);

		return ResponseEntity.noContent().build();
	}

	// 取消停用員工（恢復在職）
	@PatchMapping("/{empNo}/enable")
	public ResponseEntity<Void> enableEmployee(@PathVariable Integer empNo) {

		if (employeeService.getEmployeeById(empNo).isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		employeeService.enableEmployee(empNo);

		return ResponseEntity.noContent().build();
	}

	// 上傳或更換員工照片
	@PostMapping(value = "/{empNo}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> uploadEmployeePhoto(@PathVariable Integer empNo,
			@RequestParam("photo") MultipartFile photo) throws IOException {

		if (employeeService.getEmployeeById(empNo).isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		// 檢查是否選擇檔案
		if (photo.isEmpty()) {
			return ResponseEntity.badRequest().body("請選擇照片");
		}

		// 限制 5 MB
		if (photo.getSize() > 5 * 1024 * 1024) {
			return ResponseEntity.badRequest().body("照片不可超過 5 MB");
		}

		byte[] bytes = photo.getBytes();

		// 根據圖片內容辨識格式，不只相信副檔名
		boolean jpeg = bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8
				&& (bytes[2] & 0xff) == 0xff;

		boolean png = bytes.length >= 8
				&& Arrays.equals(Arrays.copyOf(bytes, 8), new byte[] { (byte) 137, 80, 78, 71, 13, 10, 26, 10 });

		boolean webp = bytes.length >= 12
				&& new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
				&& new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");

		if (!jpeg && !png && !webp) {
			return ResponseEntity.badRequest().body("只接受 JPG、PNG、WebP 圖片");
		}

		try {
			employeeService.updateEmployeePhoto(empNo, bytes);

			return ResponseEntity.ok("員工照片上傳成功");

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	// 刪除員工照片
	@DeleteMapping("/{empNo}/photo")
	public ResponseEntity<Void> deleteEmployeePhoto(@PathVariable Integer empNo) {

		// 確認員工是否存在
		if (employeeService.getEmployeeById(empNo).isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		try {
			// 呼叫 Service 刪除照片
			employeeService.deleteEmployeePhoto(empNo);

			// 刪除成功，回傳 HTTP 204
			return ResponseEntity.noContent().build();

		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

	// 讀取員工照片
	@GetMapping("/{empNo}/photo")
	public ResponseEntity<byte[]> getEmployeePhoto(@PathVariable Integer empNo) {

		if (employeeService.getEmployeeById(empNo).isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		byte[] photo = employeeService.getEmployeePhoto(empNo);

		if (photo == null || photo.length == 0) {
			return ResponseEntity.notFound().build();
		}

		MediaType contentType;

		if (photo.length >= 3 && (photo[0] & 0xff) == 0xff && (photo[1] & 0xff) == 0xd8 && (photo[2] & 0xff) == 0xff) {
			contentType = MediaType.IMAGE_JPEG;
		} else if (photo.length >= 8
				&& Arrays.equals(Arrays.copyOf(photo, 8), new byte[] { (byte) 137, 80, 78, 71, 13, 10, 26, 10 })) {
			contentType = MediaType.IMAGE_PNG;
		} else if (photo.length >= 12
				&& new String(photo, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
				&& new String(photo, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")) {
			contentType = MediaType.parseMediaType("image/webp");
		} else {
			return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
		}

		return ResponseEntity.ok().contentType(contentType).body(photo);
	}

}
