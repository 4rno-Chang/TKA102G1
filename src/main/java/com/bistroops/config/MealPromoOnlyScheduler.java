package com.bistroops.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bistroops.meal.model.MealService;

// 活動限定菜品自動上架、下架：每天 00:05（台灣時間）執行，伺服器啟動時也執行一次
//   下架：活動限定、上架中，參加的活動都已結束（MealRepository.offShelfEndedPromoOnly）
//   上架：活動限定、未上架，參加的活動在 24 小時內開始（MealRepository.onShelfStartedPromoOnly）
//   00:05 執行：避開活動 00:00 開始的同一時間點
//   @EnableScheduling 在 reservationdatetime/config/ReservationSlotWindowScheduler（全專案共用）
@Component
public class MealPromoOnlyScheduler {

	
	@Autowired
	private MealService mealService;

	@Scheduled(cron = "0 5 0 * * *", zone = "Asia/Taipei")     // 秒 分 時 日 月 星期 → 每天 00:05:00
	public void updatePromoOnlyMeals() {
		
		int off = mealService.offShelfEndedPromoOnly();      // 先下架：活動都結束的
		int on = mealService.onShelfStartedPromoOnly();      // 再上架：活動今天開始的
		System.out.println("活動限定菜品：自動上架 " + on + " 道、自動下架 " + off + " 道"); 
	}
	
	//監控開啟，如果專案沒關這邊會再啟動一次上下菜品
	@EventListener(ApplicationReadyEvent.class)
	public void checkOnStartup() {
		updatePromoOnlyMeals();
	}
	
}
