package com.bistroops;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.bistroops.announcement.model.AnnouncementService;
import com.bistroops.employee.model.EmployeeService;
import com.bistroops.employee.model.EmployeeVO;
import com.bistroops.reservation.model.ReservationService;

import jakarta.servlet.http.HttpSession;

//@PropertySource("classpath:application.properties") // 於https://start.spring.io建立Spring Boot專案時, application.properties文件預設已經放在我們的src/main/resources 目錄中，它會被自動檢測到
@Controller
public class IndexController_inSpringBoot {
	
	@Autowired
	private ReservationService reservationService;
	
	// @Autowired (●自動裝配)(Spring ORM 課程)
	@Autowired
	AnnouncementService annSvc;
	
	@Autowired
	private EmployeeService employeeService;
	
    // inject(注入資料) via application.properties
    @Value("${welcome.message}")
    private String message;
	
    @GetMapping("/")
    public String entry() {
        return "entry"; //view
    }
    
    @GetMapping("/bistroops")
    public String frontIndex(Model model) {
    	model.addAttribute("message", message);
        return "front/frontend_index"; //view
    }
    
    // 員工登入頁面
    @GetMapping("/staff/login")
    public String staffLogin(HttpSession session) {

        // 已登入的員工直接進入後台
        if (session.getAttribute("employeeNo") != null) {
            return "redirect:/staff";
        }

        return "staff/employee/staff_login";
    }

    @GetMapping("/staff")
    public String staffIndex(Model model, HttpSession session) {

        // 1. 檢查員工是否登入
        Integer empNo = (Integer) session.getAttribute("employeeNo");

        if (empNo == null) {
            return "redirect:/staff/login";
        }

        // 2. 查詢目前登入的員工
        EmployeeVO employee = employeeService.getEmployeeById(empNo)
                .orElse(null);

        // 員工不存在或已停用，清除登入狀態
        if (employee == null || !"1".equals(employee.getEmpStatus())) {
            session.removeAttribute("employeeNo");
            return "redirect:/staff/login";
        }

        // 3. 判斷是否需要強制修改密碼
        boolean mustChangePassword =
                !Boolean.TRUE.equals(employee.getEmpPwdChanged());

        model.addAttribute("mustChangePassword", mustChangePassword);
        
        // 顯示目前登入員工的姓名
        model.addAttribute("staffEmployeeName", employee.getEmpName());

        // 4. 載入原本後台資料
        model.addAllAttributes(
                reservationService.getStaffReservationSummary());

        return "staff/staff_index";
    }
}