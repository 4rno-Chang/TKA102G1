package com.bistroops.meal.controller;

import java.io.IOException;
import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bistroops.meal.model.MealFormDTO;
import com.bistroops.meal.model.MealService;
import com.bistroops.meal.model.MenuCoverRedisService;
import com.bistroops.meal.model.MenuService;
import com.bistroops.mealtype.model.MealtypeService;
import com.bistroops.promote.model.PromoteService;

import jakarta.validation.Valid;

// 後台菜單管理：/staff/meal（templates/staff/meal/meal_list.html）
@Controller
@RequestMapping("/staff")
public class StaffMealController {
	
	@Autowired
	private MealService mealService;
	
	@Autowired
	private MealtypeService mealtypeService;
	
	@Autowired
	private PromoteService promoteService;
	
	@Autowired
	private MenuService menuService;
	
	@Autowired
	private MenuCoverRedisService menuCoverRedisService;
	
	

	// 菜單管理頁
	@GetMapping("/meal")
	public String list(Model model) {
		putListData(model);
		
		return "staff/meal/meal_list";
	}
	
	// 菜品圖片
	@GetMapping("/meal/img")
	public ResponseEntity<byte[]> staffMealImg(@RequestParam(required = false) Integer mealNo) {
		byte[] img = (mealNo != null) ? menuService.getMealImg(mealNo) : null;
	    if (img == null || img.length == 0) {
	        return ResponseEntity.status(HttpStatus.FOUND)
	        	   .location(URI.create("/images/no-image.svg")).build(); // 沒有圖片：不回 404，改顯示「尚未上傳圖片」
	    }
	    return ResponseEntity.ok()
	            .contentType(MediaType.IMAGE_JPEG)
	            .body(img);
	}
	
	// 新增、修改菜品（mealNo 是 null 為新增）；檢查不通過時回到同一頁，對話框保留填過的內容
	@PostMapping("/meal/save")
	public String save(@Valid @ModelAttribute("mealForm") MealFormDTO form, BindingResult result,
					   @RequestParam(required = false) MultipartFile mealImg,
					   Model model, RedirectAttributes ra) throws IOException {
		
		String error = result.hasErrors() ? result.getAllErrors().get(0).getDefaultMessage() : mealService.checkForm(form, mealImg);
		if (error != null) {   // 失敗：不轉址，直接回這一頁，對話框會自己打開、填回剛剛的內容
			if (mealImg != null && !mealImg.isEmpty()) error += "（圖片請重新選擇）";
			putListData(model);
			
			model.addAttribute("formError", error);
			
			return "staff/meal/meal_list";
		}
     
		String done = (form.getMealNo() == null) ? "已新增「" : "已儲存「";
		Integer mealNo = mealService.saveMeal(form, mealImg);
		
		ra.addFlashAttribute("message", done + form.getMealName() + "」");
		ra.addAttribute("mealNo", mealNo); 

		return "redirect:/staff/meal";
	}
	
	// 設為類別代表圖（存在 Redis）
	@PostMapping("/meal/cover")
	public String mealCover(@RequestParam Integer mealtypeNo, @RequestParam Integer mealNo, RedirectAttributes ra) {
		menuCoverRedisService.setCover(mealtypeNo, mealNo);
		ra.addFlashAttribute("message", "設置成功");
		ra.addAttribute("mealNo", mealNo);   // 回到頁面時保持打開這道菜的詳細
		return "redirect:/staff/meal";
	}
	
	// 刪除類別（軟刪除），結果顯示在「編輯類別」對話框
	@PostMapping("/meal/type/delete")
	public String deleteType(@RequestParam Integer mealtypeNo, RedirectAttributes ra) {
		String error = mealtypeService.deleteMealtype(mealtypeNo);
		if (error == null) {
			ra.addFlashAttribute("typeMessage", "已刪除類別");
		} else {
			ra.addFlashAttribute("typeError", error);
			}
		return "redirect:/staff/meal";
	}
	
	// 新增、修改類別（mealtypeNo 是 null 為新增），結果顯示在「編輯類別」對話框
	@PostMapping("/meal/type/save")
	public String saveType(@RequestParam(required = false) Integer mealtypeNo,
						   @RequestParam(required = false) String mealtypeName,
						   @RequestParam(required = false) String mealtypeHashtag, RedirectAttributes ra) {
		String error = mealtypeService.saveMealtype(mealtypeNo, mealtypeName, mealtypeHashtag);
		if (error == null) {
			ra.addFlashAttribute("typeMessage", (mealtypeNo == null) ? "已新增類別" : "已儲存類別");
		} else {
			ra.addFlashAttribute("typeError", error);
		}
		return "redirect:/staff/meal";
	}
	
	// 刪除菜品（軟刪除）
	@PostMapping("/meal/delete")
	public String deleteMeal(@RequestParam Integer mealNo, RedirectAttributes ra) {
		String error = mealService.deleteMeal(mealNo);
		if (error == null) {
			ra.addFlashAttribute("message", "已刪除菜品");
		} else {
			ra.addFlashAttribute("error", error);
		}
		return "redirect:/staff/meal"; 
	}

	
	
	
	// 菜單管理頁需要的資料（打開頁面、儲存失敗回到同一頁時使用）
	private void putListData(Model model) {
		model.addAttribute("mealtypes", mealtypeService.getAllMealtype());
		model.addAttribute("meals", mealService.getRows());
		model.addAttribute("covers", menuCoverRedisService.getCovers());
		model.addAttribute("promotes", promoteService.getOpenOptions());
		model.addAttribute("mealPromos", mealService.getMealPromos());
	}
}
