package com.bistroops;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.bistroops.announcement.model.AnnouncementService;

//@PropertySource("classpath:application.properties") // 於https://start.spring.io建立Spring Boot專案時, application.properties文件預設已經放在我們的src/main/resources 目錄中，它會被自動檢測到
@Controller
public class IndexController_inSpringBoot {
	
	// @Autowired (●自動裝配)(Spring ORM 課程)
	@Autowired
	AnnouncementService annSvc;
	
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

    @GetMapping("/staff")
    public String staffIndex(Model model) {
    	// model.addAttribute("message", message);
        return "staff/staff_index"; //view
    }
}