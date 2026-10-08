package com.bistroops.member.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.bistroops.member.model.CaptchaService;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import jakarta.servlet.http.HttpServletResponse;
import com.bistroops.member.model.MemberService;
import com.bistroops.member.model.MemberVO;
import java.time.LocalDate;

import java.util.Random;
import jakarta.servlet.http.HttpSession;

@Controller
public class MemberController {
	
	@Autowired
	MemberService memberService;
	
	@Autowired
	CaptchaService captchaService;

	//會員註冊
	@PostMapping("/member/register")
	public String register(
	        @RequestParam String memTel,
	        @RequestParam String memPassword,
	        @RequestParam(required = false) String returnUrl,
	        RedirectAttributes redirectAttributes) {
		
		
		// 註冊完成或失敗後，回到使用者原本所在的前台頁面
	    if (returnUrl == null
	            || !returnUrl.startsWith("/bistroops")
	            || returnUrl.startsWith("//")) {
	        returnUrl = "/bistroops";
	    }

	    // 手機號碼不可空白
	    if (memTel == null || memTel.isBlank()) {

	        redirectAttributes.addFlashAttribute("regError", "請輸入手機號碼");
	        redirectAttributes.addFlashAttribute("startView", "register");

	        return "redirect:" + returnUrl;
	    }

	    // 手機號碼必須為 09 開頭，共 10 碼
	    if (!memTel.matches("^09\\d{8}$")) {

	        redirectAttributes.addFlashAttribute("regError", "手機號碼格式錯誤");
	        redirectAttributes.addFlashAttribute("startView", "register");

	        return "redirect:" + returnUrl;
	    }

	    // 密碼不可空白
	    if (memPassword == null || memPassword.isBlank()) {

	        redirectAttributes.addFlashAttribute("regError", "請輸入密碼");
	        redirectAttributes.addFlashAttribute("startView", "register");

	        return "redirect:" + returnUrl;
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

	        return "redirect:" + returnUrl;

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

	        return "redirect:" + returnUrl;
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
			
			// 發送手機驗證碼
			@PostMapping("/member/sendCode")
			@ResponseBody
			public String sendCode(
			        @RequestParam(required = false) String phone,
			        @RequestParam String purpose,
			        HttpSession session) {

			    // 更改密碼：手機號碼不從前端取得，而是從登入中的會員取得
			    if ("change".equals(purpose)) {

			        MemberVO member =
			                (MemberVO) session.getAttribute("member");

			        // 沒有登入
			        if (member == null) {
			            return "請先登入";
			        }

			        phone = member.getMemTel();
			    }

			    // 手機號碼格式檢查
			    if (phone == null || !phone.matches("^09\\d{8}$")) {
			        return "手機號碼格式錯誤";
			    }

			    // 忘記密碼：手機必須已經註冊
			    if ("forgot".equals(purpose)) {
			        if (!memberService.isTelRegistered(phone)) {
			            return "NOT_REGISTERED";
			        }
			    }

			    // 註冊：手機不能已經註冊
			    if ("register".equals(purpose)) {
			        if (memberService.isTelRegistered(phone)) {
			            return "☒ 手機號碼已被註冊";
			        }
			    }

			    // ===== 開發測試用驗證碼 =====
			    Random random = new Random();
			    String code = String.format("%06d", random.nextInt(1000000));

			    // 把驗證碼與對應手機暫存在 Session
			    session.setAttribute("smsCode", code);
			    session.setAttribute("smsPhone", phone);

			    // 先印在 Eclipse Console，方便測試
			    System.out.println("驗證碼發送至：" + phone);
			    System.out.println("測試驗證碼：" + code);

			    return "OK";
			}
			
			// 驗證手機驗證碼
			@PostMapping("/member/verifyCode")
			@ResponseBody
			public String verifyCode(@RequestParam String code, HttpSession session) {

			    // 取得 Session 裡真正的驗證碼
			    String smsCode = (String) session.getAttribute("smsCode");

			    // 尚未發送驗證碼
			    if (smsCode == null) {
			        return "NO_CODE";
			    }

			    // 驗證碼正確
			    if (smsCode.equals(code)) {
			        return "OK";
			    }

			    // 驗證碼錯誤
			    return "ERROR";
			}
			
			// 忘記密碼－即時驗證手機與驗證碼
			@PostMapping("/member/verifyForgotCode")
			@ResponseBody
			public String verifyForgotCode(@RequestParam String phone, @RequestParam String code, HttpSession session) {

			    // Session 裡剛才發送的驗證碼、手機
			    String smsCode = (String) session.getAttribute("smsCode");
			    String smsPhone = (String) session.getAttribute("smsPhone");

			    // 還沒發送驗證碼
			    if (smsCode == null || smsPhone == null) {
			        return "NO_CODE";
			    }

			    // 手機不一致
			    if (!smsPhone.equals(phone)) {
			        return "ERROR";
			    }

			    // 驗證碼錯誤
			    if (!smsCode.equals(code)) {
			        return "ERROR";
			    }

			    // 驗證成功，記住等等要重設密碼的手機
			    session.setAttribute("resetPhone", phone);

			    return "OK";
			}
			// 產生登入圖片驗證碼
			@GetMapping("/member/captcha")
			public void captcha(HttpSession session,
			                    HttpServletResponse response) throws IOException {

			    // 產生 4 位數驗證碼，並存進 Redis
			    String code = captchaService.createCaptcha(session.getId());

			    // 建立透明背景的驗證碼圖片
			    BufferedImage image =
			            new BufferedImage(104, 46, BufferedImage.TYPE_INT_ARGB);

			    Graphics2D g = image.createGraphics();

			    // 驗證碼文字
			    g.setColor(new Color(45, 40, 35));
			    g.setFont(new Font("italic", Font.BOLD | Font.ITALIC, 22));

			    // 每個數字分開畫，做出原本的字距效果
			    int x = 18;

			    for (char c : code.toCharArray()) {
			        g.drawString(String.valueOf(c), x, 30);
			        x += 20;
			    }

			    g.dispose();

			    // 告訴瀏覽器這是一張 PNG
			    response.setContentType("image/png");

			    // 不要快取驗證碼圖片
			    response.setHeader(
			            "Cache-Control",
			            "no-store, no-cache, must-revalidate"
			    );

			    // 把圖片送給瀏覽器
			    ImageIO.write(image, "png", response.getOutputStream());
			}		
	
			// 會員登入
			@PostMapping("/member/login")
			@ResponseBody
			public String login(@RequestParam String phone,
			                    @RequestParam String password,
			                    @RequestParam String captcha,
			                    HttpSession session) {

			    // 先驗證圖片驗證碼
			    boolean captchaCorrect =
			            captchaService.verifyCaptcha(session.getId(), captcha);

			    if (!captchaCorrect) {
			        return "CAPTCHA_ERROR";
			    }

			    // 驗證帳號、密碼
			    MemberVO member = memberService.login(phone, password);

			    if (member != null) {

			        // 登入成功，把會員資料存進 Session
			        session.setAttribute("member", member);

			        return "OK";
			    }

			    // 帳號或密碼錯誤
			    return "ERROR";
			}
			
			// 會員登出
			@GetMapping("/member/logout")
			public String logout(
			        @RequestParam(required = false) String returnUrl,
			        HttpSession session,
			        RedirectAttributes redirectAttributes) {

			    // 登出完成後，回到使用者原本所在的前台頁面
			    if (returnUrl == null
			            || !returnUrl.startsWith("/bistroops")
			            || returnUrl.startsWith("//")) {
			        returnUrl = "/bistroops";
			    }

			    // 移除 Session 中的會員資料
			    session.removeAttribute("member");

			    // 登出後顯示提示
			    redirectAttributes.addFlashAttribute(
			            "toastMsg",
			            "您已登出"
			    );

			    // 回到原本所在頁面
			    return "redirect:" + returnUrl;
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
	public String updateMember(@RequestParam String name,
	        @RequestParam String email,
	        @RequestParam(required = false) String memBarcode,
//	        @RequestParam String memTag,
	        @RequestParam(required = false) String birthYear,
	        @RequestParam(required = false) String birthMonth,
	        @RequestParam(required = false) String birthDay,
	        @RequestParam(required = false) String returnUrl,
	        HttpSession session, RedirectAttributes redirectAttributes) {
		
		// 修改會員資料完成或失敗後，回到使用者原本所在的前台頁面
		if (returnUrl == null
		        || !returnUrl.startsWith("/bistroops")
		        || returnUrl.startsWith("//")) {
		    returnUrl = "/bistroops";
		}

	    //取得目前登入會員
	    MemberVO member = (MemberVO) session.getAttribute("member");

	    //沒有登入
	    if (member == null) {
	    	return "redirect:" + returnUrl;
	    }

	    //取得目前登入會員的會員編號
	    Integer memNo = member.getMemNo();
	    
	    
	    // 手機條碼載具格式驗證
	    if (memBarcode != null && !memBarcode.isBlank()) {

	        if (!memBarcode.matches("^/[A-Za-z0-9]{7}$")) {

	            redirectAttributes.addFlashAttribute(
	                    "profileError",
	                    "※手機條碼載具格式錯誤"
	            );

	            redirectAttributes.addFlashAttribute(
	                    "startTab",
	                    "profile"
	            );

	            return "redirect:" + returnUrl;
	        }
	    }
	    
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
	    
	 // 標籤目前畫面沒有修改功能，所以保留會員原本的資料
	    String memTag = member.getMemTag();
	    
	    //呼叫 Service 修改資料
	    boolean result = memberService.updateMember(memNo, name, email, memBarcode, memTag, memBirth);

	    if (result) {
	    	
	    	// 修改成功後，同步更新 Session 裡的會員資料
	        member.setMemName(name);
	        member.setMemMail(email);
	        member.setMemBirth(memBirth);  
	        member.setMemBarcode(memBarcode);
//	        member.setMemTag(memTag);

	        session.setAttribute("member", member);
	        
	        // 修改成功提示
	        redirectAttributes.addFlashAttribute("toastMsg", "修改完成");

	    }
	        
	        
	    // 修改完回首頁
	    return "redirect:" + returnUrl;
	    
	}

	// 會員修改密碼
	@PostMapping("/member/changePassword")
	public String changePassword(@RequestParam String code, @RequestParam String password, @RequestParam String password2, @RequestParam(required = false) String returnUrl,  HttpSession session, RedirectAttributes redirectAttributes) {
		
	    // 修改密碼完成或失敗後，回到使用者原本所在的前台頁面
	    if (returnUrl == null
	            || !returnUrl.startsWith("/bistroops")
	            || returnUrl.startsWith("//")) {
	        returnUrl = "/bistroops";
	    }
		
	    // 取得目前登入會員
	    MemberVO member = (MemberVO) session.getAttribute("member");

	    // 沒有登入
	    if (member == null) {
	    	return "redirect:" + returnUrl;
	    }

	    // 取得 Session 中的驗證碼
	    String smsCode = (String) session.getAttribute("smsCode");

	    // 驗證碼不存在或輸入錯誤
	    if (smsCode == null || !smsCode.equals(code)) {

	        redirectAttributes.addFlashAttribute(
	                "changePwError",
	                "※驗證碼錯誤請重新輸入"
	        );

	        redirectAttributes.addFlashAttribute(
	                "startTab",
	                "password"
	        );

	        return "redirect:" + returnUrl;
	    }

	    // 兩次密碼不一致
	    if (!password.equals(password2)) {

	        redirectAttributes.addFlashAttribute(
	                "changePwError",
	                "※兩次輸入的密碼不相符"
	        );

	        redirectAttributes.addFlashAttribute(
	                "startTab",
	                "password"
	        );

	        return "redirect:" + returnUrl;
	    }

	    // 取得會員編號
	    Integer memNo = member.getMemNo();

	    // 修改密碼
	    boolean result = memberService.changePassword(memNo, password);

	    // 修改失敗
	    if (!result) {

	        redirectAttributes.addFlashAttribute(
	                "changePwError",
	                "※密碼修改失敗"
	        );

	        redirectAttributes.addFlashAttribute(
	                "startTab",
	                "password"
	        );

	        return "redirect:" + returnUrl;
	    }

	    // 驗證碼使用完就刪除
	    session.removeAttribute("smsCode");
	    session.removeAttribute("smsPhone");


	    redirectAttributes.addFlashAttribute(
	            "toastMsg",
	            "修改成功"
	    );

	    return "redirect:" + returnUrl;
	}
	
	// 忘記密碼－驗證手機與驗證碼
	@PostMapping("/member/forgotPassword")
	public String forgotPassword(@RequestParam String phone, @RequestParam String code, HttpSession session, RedirectAttributes redirectAttributes) {

	    // 取得剛才發送的驗證碼與手機號碼
	    String smsCode = (String) session.getAttribute("smsCode");
	    String smsPhone = (String) session.getAttribute("smsPhone");

	    // 驗證手機與驗證碼
	    if (smsCode == null
	            || smsPhone == null
	            || !smsPhone.equals(phone)
	            || !smsCode.equals(code)) {

	        redirectAttributes.addFlashAttribute(
	                "forgotCodeError",
	                "※驗證碼錯誤請重新輸入"
	        );

	        redirectAttributes.addFlashAttribute(
	                "startView",
	                "forgot"
	        );

	        return "redirect:/bistroops";
	    }

	    // 驗證成功，把要重設密碼的手機存進 Session
	    session.setAttribute("resetPhone", phone);

	    // 驗證成功後進入「設定新密碼」
	    redirectAttributes.addFlashAttribute(
	            "startView",
	            "reset"
	    );

	    return "redirect:/bistroops";
	}
	
	// 忘記密碼－設定新密碼
	@PostMapping("/member/resetPassword")
	public String resetPassword(@RequestParam String password, @RequestParam String password2, @RequestParam(required = false) String returnUrl, HttpSession session, RedirectAttributes redirectAttributes) {
		
		// 修改密碼完成或失敗後，回到使用者原本所在的前台頁面
		if (returnUrl == null
		        || !returnUrl.startsWith("/bistroops")
		        || returnUrl.startsWith("//")) {
		    returnUrl = "/bistroops";
		}
		
	    // 取得手機驗證成功後存在 Session 的手機號碼
	    String resetPhone = (String) session.getAttribute("resetPhone");

	    // 沒有經過手機驗證
	    if (resetPhone == null) {

	        redirectAttributes.addFlashAttribute(
	                "resetError",
	                "修改失敗！請重新操作"
	        );

	        redirectAttributes.addFlashAttribute(
	                "startView",
	                "forgot"
	        );

	        return "redirect:" + returnUrl;
	    }

	    // 兩次密碼不一致
	    if (!password.equals(password2)) {

	        redirectAttributes.addFlashAttribute(
	                "resetError",
	                "※兩次輸入的密碼不相符"
	        );

	        redirectAttributes.addFlashAttribute(
	                "startView",
	                "reset"
	        );

	        return "redirect:" + returnUrl;
	    }

	    // 呼叫 Service 重設密碼
	    boolean result =
	            memberService.resetPassword(resetPhone, password);

	    // 修改失敗
	    if (!result) {

	        redirectAttributes.addFlashAttribute(
	                "resetError",
	                "修改失敗！請重新操作"
	        );

	        redirectAttributes.addFlashAttribute(
	                "startView",
	                "reset"
	        );

	        return "redirect:" + returnUrl;
	    }

	    // 修改成功，清除忘記密碼流程的 Session
	    session.removeAttribute("resetPhone");
	    session.removeAttribute("smsCode");
	    session.removeAttribute("smsPhone");

	    // 回到登入畫面
	    redirectAttributes.addFlashAttribute(
	            "startView",
	            "login"
	    );

	    redirectAttributes.addFlashAttribute(
	            "toastMsg",
	            "密碼修改成功，請重新登入"
	    );

	    return "redirect:" + returnUrl;
	}

}
