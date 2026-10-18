package com.bistroops.announcement.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.bistroops.promote.model.PromoteService;
import com.bistroops.promote.model.PromoteVO;

/*
 * 前台「最新消息」：公告和活動共用同一種格式 article，列表頁（listAll.html）和內容頁（news-detail.html）只要讀 article
 *   title     標題
 *   date      日期（公告是開始時間，活動是活動開始時間）
 *   text      內文
 *   href      內容頁網址（不含 context path，模板用 @{${article.href}} 轉成完整網址）
 *   imageUrl  圖片網址；null ＝ 沒有圖
 * 公告只放已發布的，活動只放進行中的；只讀資料，不改 ishaya 的 PromoteService
 */
@Service
public class NewsArticleService {

	private final AnnouncementService announcementService;
	private final PromoteService promoteService;

	public NewsArticleService(AnnouncementService announcementService, PromoteService promoteService) {
		this.announcementService = announcementService;
		this.promoteService = promoteService;
	}

	// 列表頁：已發布的公告＋進行中的活動，依日期新到舊
	public List<Map<String, Object>> getArticles() {
		List<Map<String, Object>> articles = new ArrayList<>();
		for (AnnouncementVO announcement : announcementService.getPublished()) {
			articles.add(toArticle(announcement));
		}
		for (PromoteVO promote : getActivePromotes()) {
			articles.add(toArticle(promote));
		}
		articles.sort(Comparator.comparing((Map<String, Object> article) -> (LocalDateTime) article.get("date"),
				Comparator.nullsLast(Comparator.reverseOrder())));
		return articles;
	}

	public Map<String, Object> toArticle(AnnouncementVO announcement) {
		boolean hasImage = announcement.getAnnImg() != null && announcement.getAnnImg().length > 0;
		return article(announcement.getAnnTitle(), announcement.getAnnBegin(), announcement.getAnnText(),
				"/bistroops/announcement/" + announcement.getAnnNo(),
				hasImage ? "/bistroops/announcement/image/" + announcement.getAnnNo() : null);
	}

	public Map<String, Object> toArticle(PromoteVO promote) {
		boolean hasImage = promote.getPromoteImg() != null && promote.getPromoteImg().length > 0;
		return article(promote.getPromoteName(), promote.getPromoteBegin(), promote.getPromoteContent(),
				"/bistroops/promote/" + promote.getPromoteNo(),
				hasImage ? "/bistroops/promote/img?promoteNo=" + promote.getPromoteNo() : null);
	}

	// 進行中的活動（全部）：getPromoteByStatus 一頁只有 8 筆，一頁一頁讀到沒有下一頁；首頁輪播也用這個
	public List<PromoteVO> getActivePromotes() {
		List<PromoteVO> promotes = new ArrayList<>();
		LocalDateTime now = LocalDateTime.now();
		int page = 1;
		Page<PromoteVO> result;
		do {
			result = promoteService.getPromoteByStatus("進行中", "", now, page);
			promotes.addAll(result.getContent());
			page++;
		} while (result.hasNext());
		return promotes;
	}

	private static Map<String, Object> article(String title, LocalDateTime date, String text, String href,
			String imageUrl) {
		Map<String, Object> article = new LinkedHashMap<>();
		article.put("title", title);
		article.put("date", date);
		article.put("text", text);
		article.put("href", href);
		article.put("imageUrl", imageUrl);
		return article;
	}
}
