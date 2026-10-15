package com.bistroops.promote.model;

//用於打包菜品的活動價
// ══════════════════════════════════════════════════════════════════════════════
// 一道菜「現在」的活動價，和用的是哪一個活動 ── 小物件（不是資料表，不要加 @Entity）
//   誰產生它：PromoteService.getActivePrices()，回傳 Map<菜品編號, PromoPriceDTO>，Map 裡只有「現在有活動的菜」。
//   ★ 為什麼不直接用 PromoteMealVO：PromoteMealVO 是資料表的一列 ──「某個活動裡的某道菜賣多少」。
//     同一道菜可能同時在兩個活動裡（兩列），點餐要的是比完之後的答案：這道菜現在賣多少（取便宜的）、用哪個活動。
//     這個答案資料表裡沒有，是算出來的，所以用小物件裝。
//   ★ 為什麼放在 promote：「活動把菜調成多少錢」是活動的規則，產生它的是 PromoteService；
//     線上點餐（以及之後首頁、後台想顯示活動價）都來這裡拿，promote 不用認得 frontorder。
//   ★ 名稱：Promo（活動）+ Price（價格）+ DTO（Data Transfer Object，只用來搬資料的小物件）。
//     看到 XxxVO 是對應資料表的，看到 XxxDTO 就是小物件；這個價格是活動算出來的，不是 MealVO 的原價（mealPrice）。
//   誰用它：MenuMealCardDTO.applyPromo（菜單換成活動價）、FrontOrderService（購物車、送出訂單的單價）。
// ══════════════════════════════════════════════════════════════════════════════
public class PromoPriceDTO {
	private Integer price;
	
	private PromoteVO promote;
	//回傳是否有參加活動
	//（getActivePrices 只放有活動的菜，所以從那裡拿到的都是 true；留著給之後「沒有活動也要裝原價」的情況用）
	public boolean isPromote() { 
		return promote != null;
	}
	
	
	//此為菜單、購物車、送出訂單用，建立之後不能改(只是暫存並非存進資料庫)
	public PromoPriceDTO(Integer price,PromoteVO promote) {
		this.price = price;
		this.promote = promote;
	}

	public Integer getPrice() {
		return price;
	}
	

	public PromoteVO getPromote() {
		return promote;
	}


	
}
