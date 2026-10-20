package com.bistroops.promote.model;

// 整單折扣活動的小物件（介面投影：Spring Data 查詢時自動做出物件，只查這四欄、不帶圖片）
//   PromoteRepository 的 findPromoteOptions（結帳對話框的選項，放進 Model 的名稱是 promoteOptions）、
//   findAutoApplyPromotes（無條件自動套用要挑的活動）會用到；查詢要用 AS 別名，別名＝getter 去掉 get（例如 p.promoteNo AS promoteNo）
//   promoteDisc：整單折數，百分比 10～90（畫面上顯示成「8 折」「85 折」）；promoteRule：條件，沒有是 null（畫面上顯示「無條件」）
public interface OrderPromoteOptionDTO {
	Integer getPromoteNo();
	
	String  getPromoteName();
	
	Integer getPromoteDisc();
	
	String  getPromoteRule();

}
