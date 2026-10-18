package com.bistroops.mealtype.model;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MealtypeService {
	
	@Autowired 
	MealtypeRepository dao;
	
	// 類別刪除為軟刪除：meal_type_hashtag 改成 null；findInUse() 只查 hashtag 不是 null 的類別

	// 類別的四種種類（畫面上的「開胃菜」存成「前菜」），決定點餐時的常用備註
	public static final List<String> TAGS = List.of("前菜", "主餐", "甜點", "飲料");
	
	// 依編號查一個類別
	public MealtypeVO getMealtype(Integer mealtypeNo) {
		return dao.findById(mealtypeNo).orElse(null);
	}
	
	// 查還在用的類別
	public List<MealtypeVO> getAllMealtype(){
		return dao.findInUse();
	}

	// 新增類別
	public MealtypeVO insertMealtype(String mealtypeName , String mealtypeHashtag ) {
		MealtypeVO mealtype = new MealtypeVO();
		mealtype.setMealtypeName(mealtypeName);
		mealtype.setMealtypeHashtag(mealtypeHashtag);
		return dao.save(mealtype);
	}
	
	


	// 刪除類別
	@Transactional
	public String deleteMealtype(Integer mealtypeNo) {
		MealtypeVO type = dao.findById(mealtypeNo).orElse(null);
		if (type == null || type.getMealtypeHashtag() == null) return "查無此類別，刪除失敗";
		if (type.getMeals() != null && !type.getMeals().isEmpty()) return "這個類別還有菜品，不能刪除";
		type.setMealtypeHashtag(null);
		dao.save(type);
		return null;
	}
	
	//新增或修改(含資料檢查)
	@Transactional
	public String saveMealtype(Integer mealtypeNo, String name, String tag) {
		String error = checkMealtype(mealtypeNo, name, tag);
		if (error != null) return error;
		if (mealtypeNo == null) {
			insertMealtype(name.trim(), tag);
		} else {
			MealtypeVO t = getMealtype(mealtypeNo);
			t.setMealtypeName(name.trim());
			t.setMealtypeHashtag(tag);
			dao.save(t);
		}
		return null;
	}

	// 檢查名稱和種類：有問題回傳錯誤訊息，沒問題回傳 null（新增時 mealtypeNo 傳 null）
	public String checkMealtype(Integer mealtypeNo, String name, String tag) {
		
		String n = (name == null) ? "" : name.trim();
	
		if (n.isEmpty())          return "請輸入類別名稱";
		if (n.length() > 30)      return "類別名稱最多 30 個字";
		if (!TAGS.contains(tag))  return "請選擇種類：開胃菜、主餐、甜點或飲料";
	
		if (mealtypeNo != null) { 
			MealtypeVO t = getMealtype(mealtypeNo);
			if (t == null || t.getMealtypeHashtag() == null) return "查無此類別，請重新整理頁面";
		}
	
		// 名稱不能跟「其他還在用的類別」一樣
		boolean dup = getAllMealtype().stream()
				.anyMatch(t -> t.getMealtypeName().equals(n) && !t.getMealtypeNo().equals(mealtypeNo));
		
		if (dup) return "已經有「" + n + "」這個類別了";
		return null;
	}
}
