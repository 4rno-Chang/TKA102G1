package com.bistroops.employee.model;

import java.security.SecureRandom;
import org.springframework.stereotype.Service;

/** Sends mail only after EmployeeService's database transaction has committed. */
@Service
public class EmployeeAccountService {
    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private final SecureRandom random = new SecureRandom();
    private final EmployeeService employeeService;
    private final EmployeeMailService mailService;

    public EmployeeAccountService(EmployeeService employeeService, EmployeeMailService mailService) {
        this.employeeService = employeeService;
        this.mailService = mailService;
    }

    public record DeliveryResult(Integer empNo, boolean emailSent, String message) {}

    public DeliveryResult createEmployee(EmployeeVO employee, java.util.List<Integer> permissions) {
        String password = generateTemporaryPassword();
        EmployeeVO saved = employeeService.addEmployeeWithPermissions(employee, password, permissions);
        return deliver(saved, password, true);
    }

    public DeliveryResult resetPassword(Integer empNo) {
        String password = generateTemporaryPassword();
        EmployeeVO saved = employeeService.resetPassword(empNo, password);
        return deliver(saved, password, false);
    }

    public DeliveryResult enableEmployee(Integer empNo) {
        String password = generateTemporaryPassword();
        EmployeeVO saved = employeeService.enableEmployee(empNo, password);
        // Service 交易已提交；寄送失敗不回復成停用狀態。
        boolean sent = mailService.sendTemporaryPassword(saved, password);
        String message = sent
                ? "員工已恢復在職，臨時密碼已寄至員工信箱，請於 24 小時內登入並修改密碼"
                : "員工已恢復在職，舊密碼已失效，但通知信寄送失敗。請進入編輯補上或修改 Email 並儲存，確認寄信設定後，再按「忘記密碼」重新寄送";
        return new DeliveryResult(saved.getEmpNo(), sent, message);
    }

    String generateTemporaryPassword() {
        char[] password = new char[12];
        password[0] = LETTERS.charAt(random.nextInt(LETTERS.length()));
        password[1] = DIGITS.charAt(random.nextInt(DIGITS.length()));
        String alphabet = LETTERS + DIGITS;
        for (int i = 2; i < password.length; i++) {
            password[i] = alphabet.charAt(random.nextInt(alphabet.length()));
        }
        for (int i = password.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char swap = password[i];
            password[i] = password[j];
            password[j] = swap;
        }
        return new String(password);
    }

    private DeliveryResult deliver(EmployeeVO employee, String password, boolean created) {
        boolean sent = mailService.sendTemporaryPassword(employee, password);
        String message = sent
                ? (created ? "員工已新增" : "密碼已重設") + "，臨時密碼已寄至員工信箱，請於 24 小時內登入並修改密碼"
                : (created ? "員工已新增" : "密碼已重設，舊密碼已失效")
                    + "，但通知信寄送失敗。請確認已儲存的 Email 與寄信設定，再於編輯視窗按「忘記密碼」重新寄送，勿重複新增員工";
        return new DeliveryResult(employee.getEmpNo(), sent, message);
    }
}
