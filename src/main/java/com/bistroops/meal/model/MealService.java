package com.bistroops.meal.model;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.bistroops.ImageCompressor;
import com.bistroops.mealtype.model.MealtypeService;
import com.bistroops.mealtype.model.MealtypeVO;
import com.bistroops.promote.model.PromoteOptionDTO;
import com.bistroops.promote.model.PromoteService;
import com.bistroops.promotemeal.model.ComPromteMealId;
import com.bistroops.promotemeal.model.MealPromoDTO;
import com.bistroops.promotemeal.model.PromoteMealRepository;
import com.bistroops.promotemeal.model.PromoteMealVO;

// 後台菜單管理：菜品的查詢、檢查、儲存、刪除，活動限定菜品的自動上下架（前台菜單用的是 MenuService）
@Service
public class MealService {

	@Autowired
	MealRepository mealRepository;

	@Autowired
	PromoteMealRepository promoteMealRepository;

	@Autowired
	PromoteService promoteSve;


	@Autowired
	MealtypeService mealtypeSve;

	// 查詢所有菜品(不含軟性刪除mealtype設為null的)
	public List<MealRowDTO> getRows() {
		return mealRepository.findRows(MealEnum.ONSALE);
	}


	// 菜品參加的未結束活動與活動價（列表顯示用）
	public List<MealPromoDTO> getMealPromos() {
		return promoteMealRepository.findOpenPromos(LocalDateTime.now());
	}

	// 新增和修改共用
	@Transactional
	public Integer saveMeal(MealFormDTO form, MultipartFile img) throws IOException {
		MealVO meal = (form.getMealNo() == null) ? new MealVO()
				: mealRepository.findById(form.getMealNo()).orElseThrow();
		
		meal.setMealType(
				form.getMealtypeNo() == 0 ? newMealtype(form) : mealtypeSve.getMealtype(form.getMealtypeNo())); // 類別已在 checkForm 確認過
		meal.setMealName(form.getMealName().trim());
		meal.setMealPrice(form.getMealPrice());
		meal.setMealExp(form.getMealExp() == null ? null : form.getMealExp().trim());
		meal.setMealStatusEn("未上架".equals(form.getMealStatus()) ? MealEnum.NOTSALE : MealEnum.ONSALE);
		meal.setMealPromoOnly(form.isMealPromoOnly()); // 活動限定菜品
		
		if (img != null && !img.isEmpty()) // 選了新圖片：縮小＋壓縮後換掉
			meal.setMealImg(ImageCompressor.compress(img.getBytes(), ImageCompressor.MEAL_WIDTH));
		else if (form.isRemoveImg())
			meal.setMealImg(null); // 勾了刪除：拿掉（兩個都沒有：圖片不動）

		meal = mealRepository.save(meal);
		savePromos(meal, form.getPromos());
		return meal.getMealNo();
	}
	
	
	@Transactional
	// 刪除菜品：軟刪除（類別改成 null、改未上架、取消活動限定，拿掉還沒結束的活動）
	public String deleteMeal(Integer mealNo) {
		MealVO meal = mealRepository.findById(mealNo).orElse(null);
		if (meal == null || meal.getMealType() == null) 
			return "找不到這道菜（可能已經刪除了）";
		
		promoteMealRepository.deleteOpenByMeal(mealNo, LocalDateTime.now());   // 還沒結束的活動拿掉
		meal.setMealType(null);                  // 刪除＝類別改成 null
		meal.setMealStatusEn(MealEnum.NOTSALE);
		meal.setMealPromoOnly(false);// 取消活動限定：半夜的排程才不會又把它上架
		
		mealRepository.save(meal);
		return null;
  }
	
	// 選了「＋ 新增類別」：先建立類別
	private MealtypeVO newMealtype(MealFormDTO form) {
		return mealtypeSve.insertMealtype(form.getNewTypeName().trim(), form.getNewTypeTag());
	}

	// 檢查表單（@Valid 以外的規則）：有問題回傳錯誤訊息，沒問題回傳 null
	public String checkForm(MealFormDTO form, MultipartFile img) {
		if (form.getMealNo() != null) {
			MealVO old = mealRepository.findById(form.getMealNo()).orElse(null);
			if (old == null || old.getMealType() == null)
				return "找不到這道菜（可能已經刪除了），請重新整理頁面";
		}
		if (img != null && !img.isEmpty() && !String.valueOf(img.getContentType()).startsWith("image/"))
			return "只能上傳圖片檔";

		String mealName = form.getMealName().trim();

		if (mealRepository.existsActiveName(mealName, form.getMealNo()))
			return "已經有一道叫「" + mealName + "」的菜了，請換一個名稱";
		String promoError = checkPromos(form);
		if (promoError != null)
			return promoError;

		if (form.getMealtypeNo() != 0) {
			return mealtypeSve.getAllMealtype().stream()
						  .anyMatch(t -> t.getMealtypeNo()
						  .equals(form.getMealtypeNo())) ? null: "找不到這個類別";
		}
		return mealtypeSve.checkMealtype(null, form.getNewTypeName(), form.getNewTypeTag());
	}

	// 活動價：有勾的活動要有金額，0 元（贈送／免費）可以，不能是負數、不能超過原價
	private String checkPromos(MealFormDTO form) {
		boolean anyJoined = form.getPromos() != null && form.getPromos().stream().anyMatch(MealPromoFormDTO::isJoined);
		if (form.isMealPromoOnly() && !anyJoined)
			return "活動限定菜品至少要參加一個活動";
		if (form.getPromos() == null)
			return null;
		Set<Integer> openNos = openPromoteNos();

		// 新增前確認是否活動存在且進行中
		for (MealPromoFormDTO p : form.getPromos()) {
			if (!p.isJoined())
				continue;
			if (!openNos.contains(p.getPromoteNo()))
				return "有勾選的活動已經結束了，請重新整理頁面後再儲存";

			if (p.getPrice() == null || p.getPrice() < 0)
				return "請填活動價（不能是負數）";

			if (p.getPrice() > form.getMealPrice())
				return "活動價不能比原價高";
		}

		return null;
	}

	// 現在還沒結束的活動編號
	private Set<Integer> openPromoteNos() {
		return promoteSve.getOpenOptions().stream().map(PromoteOptionDTO::getPromoteNo).collect(Collectors.toSet());
	}
	
	// 儲存活動價：只處理還沒結束的活動；有勾存檔，沒勾刪除
	private void savePromos(MealVO meal, List<MealPromoFormDTO> promos) {
		if (promos == null) return;

		Set<Integer> openNos = openPromoteNos();
		
		for (MealPromoFormDTO p : promos) {
			if (!openNos.contains(p.getPromoteNo())) continue;
			ComPromteMealId id = new ComPromteMealId(p.getPromoteNo(), meal.getMealNo());
			if (!p.isJoined()) {
				promoteMealRepository.deleteById(id);
				continue;
			}
		
			PromoteMealVO pm = new PromoteMealVO();
		
			pm.setId(id);
			pm.setPromote(promoteSve.getPromoteByNo(p.getPromoteNo()));
			pm.setMeal(meal);
			pm.setPromoteDiscount(p.getPrice());
		
			promoteMealRepository.save(pm);
		}
	}
	
	//活動限定菜品，自動下架
	@Transactional
	public int offShelfEndedPromoOnly() {
		return mealRepository.offShelfEndedPromoOnly(MealEnum.ONSALE, MealEnum.NOTSALE, 
													 LocalDateTime.now());
	}
	
	//活動限定菜品，自動上架
	@Transactional
	public int onShelfStartedPromoOnly() {
		LocalDateTime now = LocalDateTime.now();
		return mealRepository.onShelfStartedPromoOnly(MealEnum.ONSALE, MealEnum.NOTSALE,
														now, now.minusDays(1));
  }
	
}