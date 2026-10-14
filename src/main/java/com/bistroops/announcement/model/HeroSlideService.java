package com.bistroops.announcement.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

// 活動接上時要打開的 import（見下面的【接活動】）
// import com.bistroops.promote.model.PromoteService;
// import com.bistroops.promote.model.PromoteVO;

/*
 * 首頁 hero 的輪播（frontend_index.html 用 ${@heroSlideService.getHeroSlides()} 直接呼叫）
 * 把公告（之後加上活動）整理成同一種格式，模板只要一個 th:each：
 *   title   標題（顯示在圖片下方的標題條）
 *   href    點了去哪（不含 context path，模板用 @{${slide.href}} 轉成完整網址）
 *   imgSrc  圖片網址；null ＝ 沒有圖，模板改顯示 logo
 *   date    排序用的日期（新的在前）
 *   kind    announcement 公告／promote 活動／empty 空位圖／reservation 訂位圖（後兩種圖上已經有字，不加標題條）
 * 規則：
 *   1. 公告＋活動依日期新到舊，最多 MAX 張
 *   2. 某一種一則都沒有時，放它的空位圖（也算在 MAX 張裡）
 *   3. 不足 MAX 張時，最後補一張訂位圖；滿了就不放
 * 只讀資料，不改任何組員的程式
 */
@Service
public class HeroSlideService {

	private static final int MAX = 5;

	private final AnnouncementService announcementService;
	// 【接活動】打開這行，並把下面的建構子換成有 PromoteService 的版本
	// private final PromoteService promoteService;

	public HeroSlideService(AnnouncementService announcementService) {
		this.announcementService = announcementService;
	}

	// 【接活動】換成這個建構子（Spring 會自動注入 ishaya 的 PromoteService）
	// public HeroSlideService(AnnouncementService announcementService, PromoteService promoteService) {
	// 	this.announcementService = announcementService;
	// 	this.promoteService = promoteService;
	// }

	public List<Map<String, Object>> getHeroSlides() {
		List<Map<String, Object>> contents = new ArrayList<>();
		List<Map<String, Object>> empties = new ArrayList<>();

		// 1. 公告：已發布的（開始時間不晚於現在）
		List<AnnouncementVO> announcements = announcementService.getPublished();
		for (AnnouncementVO announcement : announcements) {
			boolean hasImage = announcement.getAnnImg() != null && announcement.getAnnImg().length > 0;
			contents.add(slide(announcement.getAnnTitle(),
					"/bistroops/announcement/" + announcement.getAnnNo(),
					hasImage ? "/bistroops/announcement/image/" + announcement.getAnnNo() : null,
					announcement.getAnnBegin(), "announcement"));
		}
		if (announcements.isEmpty()) {
			empties.add(slide("最新消息", "/bistroops/announcement", "/images/slide-no-announcement.svg", null, "empty"));
		}

		// 2. 【接活動】ishaya 做好前台活動頁和前台的活動圖片網址後，打開這一段，
		//    把 "/bistroops/promote/" 換成實際的活動頁網址、"/bistroops/promote/img?promoteNo=" 換成實際的圖片網址
		// List<PromoteVO> promotes = promoteService.getPromoteByStatus("進行中", "", LocalDateTime.now(), 1).getContent();
		// for (PromoteVO promote : promotes) {
		// 	boolean hasImage = promote.getPromoteImg() != null && promote.getPromoteImg().length > 0;
		// 	contents.add(slide(promote.getPromoteName(),
		// 			"/bistroops/promote/" + promote.getPromoteNo(),
		// 			hasImage ? "/bistroops/promote/img?promoteNo=" + promote.getPromoteNo() : null,
		// 			promote.getPromoteBegin(), "promote"));
		// }
		// if (promotes.isEmpty()) {
		// 	empties.add(slide("優惠活動", "/bistroops/promote", "/images/slide-no-promote.svg", null, "empty"));
		// }

		// 3. 依日期新到舊，取前 MAX 張；空位圖接在後面，也算在 MAX 張裡
		contents.sort(Comparator.comparing((Map<String, Object> slide) -> (LocalDateTime) slide.get("date"),
				Comparator.nullsLast(Comparator.reverseOrder())));
		List<Map<String, Object>> slides = new ArrayList<>(contents.subList(0, Math.min(MAX, contents.size())));
		for (Map<String, Object> empty : empties) {
			if (slides.size() < MAX) {
				slides.add(empty);
			}
		}

		// 4. 不足 MAX 張，最後補訂位圖
		if (slides.size() < MAX) {
			slides.add(slide("線上預約訂位現正開放", "/bistroops/reservation", "/images/promo-reservation.svg", null,
					"reservation"));
		}
		return slides;
	}

	private static Map<String, Object> slide(String title, String href, String imgSrc, LocalDateTime date,
			String kind) {
		Map<String, Object> slide = new LinkedHashMap<>();
		slide.put("title", title);
		slide.put("href", href);
		slide.put("imgSrc", imgSrc);
		slide.put("date", date);
		slide.put("kind", kind);
		return slide;
	}
}
