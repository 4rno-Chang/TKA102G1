package com.bistroops.employee.controller;

import java.util.List;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

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
	public ResponseEntity<EmployeeVO> getEmployeeById(@PathVariable Integer empNo) {

		return employeeService.getEmployeeById(empNo).map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	// 查詢指定員工擁有的權限
	@GetMapping("/{empNo}/permissions")
	public ResponseEntity<List<Integer>> getEmployeePermissions(@PathVariable Integer empNo) {

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
	public ResponseEntity<String> updateEmployeePermissions(@PathVariable Integer empNo,
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
	public ResponseEntity<Integer> addEmployee(@RequestBody EmployeeVO employee) {

		// 檢查姓名
		if (employee.getEmpName() == null || employee.getEmpName().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "員工姓名不可空白");
		}

		try {
			// 新增員工
			EmployeeVO savedEmployee = employeeService.addEmployee(employee);

			// 回傳 MySQL 自動產生的員工編號
			return ResponseEntity.status(HttpStatus.CREATED).body(savedEmployee.getEmpNo());

		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
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
	public ResponseEntity<Void> disableEmployee(@PathVariable Integer empNo) {

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