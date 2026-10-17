package com.bistroops.promote.model;

import java.time.LocalDateTime;

// 後台菜單管理對話框中可參加的活動（未結束、不含圖片）
// 介面映射：getter 對應 PromoteRepository.findOpenOptions 查詢裡 AS 的別名
public interface PromoteOptionDTO {
	Integer getPromoteNo();
	
	String getPromoteName();
	
	LocalDateTime getPromoteBegin();
	
	LocalDateTime getPromoteEnd();

}
