package com.bistroops;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.bistroops.announcement.model.AnnouncementService;
import com.bistroops.announcement.model.HeroSlideService;
import com.bistroops.employee.model.EmployeeService;
import com.bistroops.employee.model.EmployeeVO;
import com.bistroops.meal.model.MenuCoverRedisService;
import com.bistroops.meal.model.MenuMealCardDTO;
import com.bistroops.meal.model.MenuService;
import com.bistroops.member.model.MemberService;
import com.bistroops.member.model.MemberVO;
import com.bistroops.promote.model.PromoteService;
import com.bistroops.qa.model.QaService;
import com.bistroops.reservation.model.ReservationService;

import jakarta.servlet.http.HttpSession;


//@PropertySource("classpath:application.properties") // 於https://start.spring.io建立Spring Boot專案時, application.properties文件預設已經放在我們的src/main/resources 目錄中，它會被自動檢測到
@Controller
public class IndexController_inSpringBoot {
	
	@Autowired
	private MemberService memberService;
	
	@Autowired
	private ReservationService reservationService;
	
	// @Autowired (●自動裝配)(Spring ORM 課程)
	@Autowired
	AnnouncementService annSvc;
	
	@Autowired
	private EmployeeService employeeService;
	
	@Autowired
	private HeroSlideService heroSlideService;   // 首頁輪播（公告，之後加上活動）

	@Autowired
	private QaService qaService;                 // 首頁常見問題（點擊最多的 5 題）
	
	@Autowired 
	private MenuService menuService;                     // 首頁精選菜單的圖片（和菜單頁同一套）
	
	@Autowired 
	private MenuCoverRedisService menuCoverRedisService;
	
	@Autowired 
	private PromoteService promoteService;


	
    // inject(注入資料) via application.properties
    @Value("${welcome.message}")
    private String message;
	
    @GetMapping("/")
    public String entry() {
        return "entry"; //view
    }
    
    @GetMapping("/bistroops")
    public String frontIndex(Model model, HttpSession session) {

        model.addAttribute("message", message);
        
        // 先在這裡查好：資料庫出錯時會顯示 500 錯誤頁（在模板裡查的話，畫面送出一半才出錯，只會出現半頁）
        model.addAttribute("heroSlides", heroSlideService.getHeroSlides());
        model.addAttribute("popularQaList", qaService.getPopular(5));
        // 精選菜單：和 FrontMenuController.menu 一樣準備類別和「類別代表」
        List<MenuMealCardDTO> cards = menuService.getMenuCards(promoteService.getActivePrices());
        model.addAttribute("menuSections", menuService.getMenuSections(cards));
        model.addAttribute("menuCovers", menuCoverRedisService.getCovers());


        Integer memNo = (Integer) session.getAttribute("memberNo");

        if (memNo != null) {
            MemberVO member = memberService.getMemNoQuery(memNo);
            model.addAttribute("member", member);
        }

        return "front/frontend_index";
    }
    
    // 員工登入頁面
    @GetMapping("/staff/login")
    public String staffLogin(HttpSession session, Model model) {

        Integer empNo = (Integer) session.getAttribute("employeeNo");
        Long expireAt = (Long) session.getAttribute("employeeExpireAt");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 保留有效登入，顯示目前身分，讓員工自行選擇返回後台或切換帳號。
        if (empNo != null && expireAt != null && System.currentTimeMillis() < expireAt
                && auth != null && auth.isAuthenticated()
                && empNo.toString().equals(auth.getName())
                && auth.getAuthorities().stream().anyMatch(a -> "EMPLOYEE".equals(a.getAuthority()))) {
            EmployeeVO employee = employeeService.getEmployeeById(empNo).orElse(null);
            if (employee != null && "1".equals(employee.getEmpStatus())) {
                model.addAttribute("loggedInEmployeeName", employee.getEmpName());
                model.addAttribute("loggedInEmployeeNo", employee.getEmpNo());
                return "staff/employee/staff_login";
            }
        }

        // 清除殘留的員工登入資訊（例如舊版 Session 只有 employeeNo、沒有 Security 身分）
        session.removeAttribute("employeeNo");
        session.removeAttribute("employeeExpireAt");
        session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        SecurityContextHolder.clearContext();

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
