package com.bistroops.member.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bistroops.member.model.MemberService;
import com.bistroops.member.model.MemberVO;
import java.time.LocalDate;

import jakarta.servlet.http.HttpSession;

@Controller
public class MemberController {
	
	@Autowired
	MemberService memberService;
	

	//會員註冊
	@PostMapping("/member/register")
	public String register(
	        @RequestParam String memTel,
	        @RequestParam String memPassword,
	        RedirectAttributes redirectAttributes) {

	    // 手機號碼不可空白
	    if (memTel == null || memTel.isBlank()) {

	        redirectAttributes.addFlashAttribute("regError", "請輸入手機號碼");
	        redirectAttributes.addFlashAttribute("startView", "register");

	        return "redirect:/bistroops";
	    }

	    // 手機號碼必須為 09 開頭，共 10 碼
	    if (!memTel.matches("^09\\d{8}$")) {

	        redirectAttributes.addFlashAttribute("regError", "手機號碼格式錯誤");
	        redirectAttributes.addFlashAttribute("startView", "register");

	        return "redirect:/bistroops";
	    }

	    // 密碼不可空白
	    if (memPassword == null || memPassword.isBlank()) {

	        redirectAttributes.addFlashAttribute("regError", "請輸入密碼");
	        redirectAttributes.addFlashAttribute("startView", "register");

	        return "redirect:/bistroops";
	    }

	    // 呼叫 Service 註冊
	    boolean isRegistered =
	            memberService.insertMember(memTel, memPassword);

	    if (isRegistered) {

	        // 註冊成功 → 回首頁後打開登入畫面
	        redirectAttributes.addFlashAttribute(
	                "toastMsg",
	                "註冊成功，請登入"
	        );

	        redirectAttributes.addFlashAttribute(
	                "startView",
	                "login"
	        );

	        return "redirect:/bistroops";

	    } else {

	        // 手機號碼已經註冊 → 回到註冊畫面
	        redirectAttributes.addFlashAttribute(
	                "regError",
	                "手機號碼已註冊"
	        );

	        redirectAttributes.addFlashAttribute(
	                "startView",
	                "register"
	        );

	        return "redirect:/bistroops";
		    }
		}
			// 檢查手機號碼是否已註冊
			@GetMapping("/member/checkPhone")
			@ResponseBody
			public String checkPhone(@RequestParam String memTel) {
		
			    boolean isRegistered = memberService.isTelRegistered(memTel);
		
			    if (isRegistered) {
			        return "TAKEN";
			    } else {
			        return "OK";
			    }
			}

	
	//會員登入
	@PostMapping("/member/login")
	public String login(@RequestParam String phone, @RequestParam String password, HttpSession session) {
	    // 依手機號碼查詢會員
	    MemberVO member = memberService.login(phone, password);
	    
	    if (member != null) {
	    	
	    	//登入成功，把會員資料存進Session
	        session.setAttribute("member", member);
	        
	        return "redirect:/bistroops";
	    	
	    } else {
	    	
	    	//登入失敗
	        return "redirect:/bistroops";
	    }
	}
	
	// 會員登出
	@GetMapping("/member/logout")
	public String logout(
	        HttpSession session,
	        RedirectAttributes redirectAttributes) {

	    // 移除 Session 中的會員資料
	    session.removeAttribute("member");

	    // 登出後顯示提示
	    redirectAttributes.addFlashAttribute(
	            "toastMsg",
	            "您已登出"
	    );

	    // 回到前台首頁
	    return "redirect:/bistroops";
	}
	

	//取得目前登入會員資料
	@GetMapping("/member/profile")  
	@ResponseBody
	public MemberVO getProfile(HttpSession session) {

	    //從 Session 取得目前登入會員
	    MemberVO member = (MemberVO) session.getAttribute("member");

	    //如果沒有登入
	    if (member == null) {
	        return null;
	    }

	    //回傳目前登入會員資料
	    return member;
	}
	
	//會員修改個人資料
	@PostMapping("/member/update")
	public String updateMember(
	        @RequestParam String name,
	        @RequestParam String email,
//	        @RequestParam String memBarcode,
//	        @RequestParam String memTag,
	        @RequestParam(required = false) String birthYear,
	        @RequestParam(required = false) String birthMonth,
	        @RequestParam(required = false) String birthDay,
	        HttpSession session) {

	    //取得目前登入會員
	    MemberVO member = (MemberVO) session.getAttribute("member");

	    //沒有登入
	    if (member == null) {
	    	return "redirect:/bistroops";
	    }

	    //取得目前登入會員的會員編號
	    Integer memNo = member.getMemNo();
	    
	    // 處理生日
	    LocalDate memBirth = null;

	    if (birthYear != null && !birthYear.isBlank()
	            && birthMonth != null && !birthMonth.isBlank()
	            && birthDay != null && !birthDay.isBlank()) {

	        memBirth = LocalDate.of(
	                Integer.parseInt(birthYear),
	                Integer.parseInt(birthMonth),
	                Integer.parseInt(birthDay)
	        );
	    }
	    
	    // 載具、標籤目前畫面沒有修改功能
	    // 所以保留會員原本的資料
	    String memBarcode = member.getMemBarcode();
	    String memTag = member.getMemTag();

	    //呼叫 Service 修改資料
	    boolean result = memberService.updateMember(memNo, name, email, memBarcode, memTag, memBirth);

	    if (result) {
	    	
	    	// 修改成功後，同步更新 Session 裡的會員資料
	        member.setMemName(name);
	        member.setMemMail(email);
	        member.setMemBirth(memBirth);  
//	        member.setMemBarcode(memBarcode);
//	        member.setMemTag(memTag);

	        session.setAttribute("member", member);
	    }
	        
	        
	    // 修改完回首頁
	    return "redirect:/bistroops";
	    
	}
	
	//會員修改密碼
	@PostMapping("/member/changePassword")
	@ResponseBody
	public String changePassword(
	        @RequestParam String oldPassword,
	        @RequestParam String newPassword,
	        HttpSession session) {

	    //取得目前登入會員
	    MemberVO member = (MemberVO) session.getAttribute("member");

	    //沒有登入
	    if (member == null) {
	        return "請先登入";
	    }

	    //取得目前登入會員的會員編號
	    Integer memNo = member.getMemNo();

	    //呼叫 Service 修改密碼
	    boolean result = memberService.changePassword(memNo, oldPassword, newPassword);

	    if (result) {
	        return "密碼修改成功";
	    } else {
	        return "目前密碼錯誤，修改失敗";
	    }
	}

}
