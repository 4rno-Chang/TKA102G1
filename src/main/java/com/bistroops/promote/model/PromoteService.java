package com.bistroops.promote.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



/*
 * 活動的 Service：放「規則」的地方。Controller 只負責收參數、放 Model、決定回哪一頁；
 * 怎麼查、能不能改、日期怎麼補、圖片要不要換，這些判斷寫在這裡，Controller 才不會愈寫愈長。
 *
 * ── 活動管理的說明放在哪裡（每件事只寫在一個地方）──
 *   PromoteController.java   有哪些網址、每個方法收什麼參數、放什麼進 Model、回傳哪一頁（串接從那裡開始看）
 *   PromoteService.java      （這個檔案）規則：日期與時間、能不能修改、圖片怎麼存；還沒寫的功能清單
 *   PromoteRepository.java   查詢怎麼寫；★ 之後的優化「列表不讀圖片（回傳小物件）」
 *   promote_list.html、promote_form.html   畫面用到哪些 Model 的名稱和欄位、假資料要刪哪裡
 *   promote.js、promote.css  畫面上的檢查、日期選擇、圖片預覽、樣式
 *
 * 【目前的狀態】（對照下面的功能清單檢查過，功能 1～8 都有對應的方法了）
 *   方法                        對應的功能                說明
 *   getPromoteByStatus          1 查詢某一頁              關鍵字是 null、頁數小於 1 都處理了
 *   getPromoteByNo              2 用編號查一筆            編號是 null 或查不到，都回傳 null
 *   getStatus                   3 判斷狀態
 *   addPromote                  4 新增                    編號設成 null（保證是新增）、時間設成 00:00 和 23:59
 *   checkNotStartPromoteDate    4、5 日期的檢查           新增、修改「未開始」的活動用（三條都檢查）
 *   updatePromote               5 修改                    進行中的活動不會動到開始時間；已結束或查不到回傳 null
 *   checkActivePromoteDate      5 日期的檢查              修改「進行中」的活動用（不檢查開始日期，結束日期和資料庫原本的開始日期比）
 *   getPromoteImgByNo           6 取得圖片                沒有那一筆回傳 null
 *   getPromoteByEnd             7 套用已有活動的搜尋      關鍵字是空的回傳空的結果，不會去查
 *   deleteNotStartPromote       8 刪除未開始的活動        活動商品會一起刪；不能刪的時候回傳原因的文字，成功回傳 null
 *   deletePromoteByNo           （備用）直接刪除          ★ 沒有任何檢查，不要接到畫面上（見下面）
 *
 *   Controller 呼叫的時候要注意：
 *     ・修改要先判斷用哪一個日期檢查：先用 getPromoteByNo 查出資料庫原本那一筆，用 getStatus 判斷，
 *       「未開始」呼叫 checkNotStartPromoteDate，「進行中」呼叫 checkActivePromoteDate。
 *       用錯的話：進行中的活動拿去用 checkNotStartPromoteDate，會一直被「開始日期不能早於今天」擋掉。
 *     ・兩個檢查日期的方法裡，表單送來的開始、結束時間不能是 null（裡面直接呼叫了它們的方法）。
 *       一定要在 Controller 的 @Valid 通過之後才呼叫。
 *     ・檢查要在 updatePromote 之前呼叫。updatePromote 自己只擋「查不到」和「已結束」，不檢查日期。
 *     ・updatePromote 回傳 null 同時代表「查無此活動」和「已經結束」，Controller 分不出是哪一種，
 *       只能顯示同一句訊息（例如「這個活動不存在或已經結束，不能修改」）。
 *     ・deleteNotStartPromote：活動底下的「活動商品」會跟著一起刪（PromoteVO 的 promoteMeals 設定了 cascade）。
 *       「訂單明細」不會刪：已經有訂單用到的活動不能刪除，會回傳原因。
 *     ・deletePromoteByNo 是直接刪除：不看狀態，遇到有訂單用到的活動資料庫會拒絕、丟出例外。
 *       畫面上的「刪除」請接 deleteNotStartPromote，不要接這一個。
 *
 * 【這裡要提供的功能】（方法的名稱、參數請自己決定，下面只寫目標）
 *
 *   1. 查詢某一頁的活動（給卡片用）　✔ 已經寫好（getPromoteByStatus）
 *      ★ 之後的優化：改成不讀圖片的查詢時，這四個方法的回傳型別要跟著改（做法在 PromoteRepository.java）。
 *
 *   2. 用編號查一筆活動（給「活動詳細」、修改的表單、輸出圖片用）　✔ 已經寫好（getPromoteByNo）
 *
 *   3. 判斷一筆活動的狀態　✔ 已經寫好（getStatus）
 *      現在 早於 開始時間 → 未開始；現在 晚於 結束時間 → 已結束；其他 → 進行中。
 *      比的是「完整的時間」（開始是 00:00、結束是 23:59），所以結束日期當天整天都還是進行中。
 *      畫面上顯示的狀態 HTML 已經自己判斷了；這裡是 Java 要「擋」的時候用（見 5 和 7）。
 *      開始或結束時間可能是 null（資料表沒有限制），判斷前要先檢查。
 *
 *   4. 新增活動
 *      名稱不能空白、不能超過 10 個字、內容不能超過 500 個字、日期必填：這幾條由 Controller 的 @Valid 檢查
 *      （標註寫在 PromoteVO 的欄位上），這裡不用再寫。
 *      這裡要檢查的是 @Valid 做不到的：日期的先後和今天（照下面的規則）。
 *      通過之後把時間設成 00:00 和 23:59 再存。有上傳圖片才設定圖片；沒有上傳就是沒有圖片。
 *      ✔ 已經寫好：addPromote（存檔）、checkNotStartPromoteDate（日期的檢查）。
 *      順序是：Controller 的 @Valid 通過 → checkNotStartPromoteDate 沒有回傳訊息 → addPromote。
 *
 *   5. 修改活動
 *      ★ 一定要先用編號查出「原本那一筆」，改它的欄位再存。
 *        不要 new 一個新的來存：沒有設定到的欄位（尤其是圖片）會被存成空的，原本的圖片就不見了。
 *      ★ 已經結束的活動不能修改。要看「資料庫裡原本的結束時間」，不是表單送來的新日期
 *        （不然把日期改晚一點就能繞過去）。畫面只是把按鈕藏起來，網址和表單還是可以自己送，所以這裡要擋。
 *      ★ 進行中的活動不能修改開始時間（見下面規則的 (c)）。
 *      圖片：有上傳新的才換；沒有上傳就保留原本的。其他檢查和新增相同。
 *      日期的檢查：未開始的活動用 checkNotStartPromoteDate（和新增一樣三條都檢查）；
 *                  進行中的活動用 checkActivePromoteDate（不檢查開始日期）。
 *      ✔ 已經寫好：updatePromote、checkActivePromoteDate。
 *
 *   6. 取得活動的圖片（給輸出圖片的網址用）　✔ 已經寫好（getPromoteImgByNo）
 *      查不到活動，或圖片是 null、長度是 0，都要讓 Controller 知道「沒有圖片」。
 *
 *   7. 套用已有活動（★ 最後再做：其他都完成、確認沒問題之後）　✔ 搜尋已經寫好（getPromoteByEnd）
 *      只套用「活動名稱」和「活動內容」，不套用圖片（新的活動需要圖片的話，使用者自己上傳）。
 *      a. 搜尋可以套用的活動：只找「已經結束」的，名稱裡有關鍵字的，只拿前面幾筆（getPromoteByEnd）。
 *         ★ 關鍵字是空的時候不要查，直接給空的清單（畫面打開時不先列出任何活動）。
 *      b. 套用：用來源的編號查出那一筆，確認它真的已經結束，再把活動名稱、活動內容帶給新增的表單
 *         （new 一個新的 PromoteVO，只設定這兩個欄位）。
 *      不用處理圖片，存檔的方法也不用為了這個功能修改。
 *
 *   8. 刪除活動（只有「未開始」的可以刪除）　✔ 已經寫好（deleteNotStartPromote）
 *      列表頁「活動詳細」的「刪除」會用到。畫面只在未開始時顯示按鈕，但表單可以自己送，所以這裡要擋：
 *        ・用編號查出那一筆；查不到 → 不刪，讓 Controller 知道（例如回傳「查無此活動」）。
 *        ・用 getStatus 判斷；不是「未開始」→ 不刪（回傳「只有未開始的活動可以刪除」）。
 *        ・通過才刪除。成功時回傳 null，Controller 就知道可以顯示「已刪除活動」。
 *      ★ 活動底下還有別的資料指著它的時候，資料庫會拒絕刪除（外來鍵）。兩種資料的處理方式不一樣：
 *        ・活動商品（promote_meal）：跟著活動一起刪。
 *          做法是在 PromoteVO 的 promoteMeals 上設定 cascade = CascadeType.REMOVE，
 *          刪除活動時 Hibernate 會先刪它的活動商品、再刪活動。資料表不用改。
 *          要注意：一定要用「先查出來、再刪那個物件」的寫法（repository.delete(查出來的活動)），cascade 才會生效；
 *          自己寫 @Query 的刪除語法不會連帶刪。方法上要有 @Transactional，兩個刪除才會一起成功或一起失敗。
 *        ・訂單明細（orders_details）：不刪。那是客人點過的紀錄，刪掉的話訂單的品項和金額會對不上。
 *          未開始的活動正常不會有訂單用到它；萬一有，就不刪除並回傳原因。
 *
 *
 * ════ ★ 日期與時間的規則（新增、修改都要用，建議寫成一個共用的檢查）════
 *   畫面只讓使用者選「年月日」。表單送來的是完整的時間（開始 00:00、結束 23:59，畫面上的 JS 補的），
 *   Controller 用 PromoteVO 接，所以這裡拿到的開始、結束是 LocalDateTime。
 *   畫面上已經擋了不對的日期，但畫面的檢查可以被跳過，這裡才是真的。
 *
 *   先把日期拿出來：時間.toLocalDate() 會得到只有年月日的 LocalDate，下面的檢查都用它來比。
 *
 *   (a) 設定時間：存檔之前，Java 要自己把時間設成固定的幾點幾分（不要相信表單送來的幾點幾分，它可以被改）：
 *         開始時間 ＝ 開始日期的 00:00（日期.atStartOfDay()）；結束時間 ＝ 結束日期的 23:59（日期.atTime(23, 59)）。
 *       例如開始選 10/15、結束選 10/16，存進去的就是 10/15 00:00 到 10/16 23:59。
 *   (b) 檢查：
 *         ・開始日期、結束日期都要有。
 *         ・開始日期不能早於今天。
 *         ・結束日期不能早於今天，而且最早是開始日期的「隔天」（不能和開始日期同一天，也不能更早）。
 *       「今天」用伺服器的日期，不要相信表單送來的任何「現在是幾號」。
 *       比的是「日期」，不是時間：今天下午新增一個今天開始的活動是可以的；
 *       不要拿 00:00 的開始時間去和「現在」比，那樣今天永遠都算太早。
 *       「開始、結束日期都要有」這一條 @Valid 已經檢查過了（PromoteVO 的 @NotNull），這裡可以當作一定有值。
 *       畫面上（promote.js 的 checkPromoteForm）用的是同樣的規則，兩邊要一樣。
 *   (c) 修改「進行中」的活動：不能改開始時間。
 *       判斷是不是進行中，要用資料庫裡原本的開始、結束時間（現在不早於開始、也不晚於結束）。是的話：
 *         ・不管表單送來什麼開始日期，都不要用它，保留原本的開始時間
 *           （畫面上那一格是鎖住的，但表單還是會送值，而且可以被改）。
 *         ・跳過「開始日期不能早於今天」那一條（它的開始日期本來就在今天以前）。
 *         ・結束日期一樣要檢查：不能早於今天，而且最早是（原本的）開始日期的隔天。
 *       未開始的活動：開始、結束日期都可以改，規則和新增相同。
 *   (d) 已經結束的活動不能修改（用資料庫原本的結束時間判斷）。
 *   (e) 每一條不通過，都要有對應的錯誤訊息可以交給 Controller 顯示。文字可以和畫面上的一樣：
 *       「請選擇開始日期」「請選擇結束日期」「開始日期不能早於今天」「結束日期不能早於今天」「結束日期最早是開始日期的隔天」。
 *
 *   顯示和查詢都不受影響：畫面一律只顯示年月日（HTML 已經處理），放進 Model 的還是原本的 LocalDateTime；
 *   查詢和狀態還是用完整的時間和現在比。
 *
 *
 * 【要注意】
 *   ・檢查不通過時，要有辦法把「錯誤訊息」交給 Controller 顯示在表單上
 *     （例如回傳訊息文字、沒有錯誤時回傳 null，做法可以自己決定）。
 *   ・檢查要在「改欄位之前」全部做完，不通過就直接回傳。
 *     從資料庫查出來的物件只要欄位被改過，之後可能會被自動存進資料庫（就算沒有呼叫存檔的方法），
 *     先改再檢查的話，不通過的資料也可能被存進去。
 *   ・圖片：收到什麼就存什麼，不用轉檔、不用改大小，取出來也是原樣交出去。
 *     畫面上固定用 16:9 顯示是 CSS 做的，和這裡無關。大小的限制寫在 PromoteController.java 的步驟 6。
 *   ・讀取上傳檔案的內容可能會丟出 IOException，要決定在這裡處理，還是往外丟給 Controller。
 *   ・新增、修改這種會寫入資料庫的方法，建議加上 @Transactional（中途出錯時不會只存一半）。
 */
@Service
public class PromoteService {

	@Autowired
	PromoteRepository repository;
	
	
	// 新增一筆活動
	@Transactional
	public PromoteVO addPromote(PromoteVO promote, byte[] img) {
	    LocalDate begin = promote.getPromoteBegin().toLocalDate();
	    LocalDate end = promote.getPromoteEnd().toLocalDate();
	    // 新增一定沒有編號。編號是表單的隱藏欄位送來的，可以被改；
	    // 設成 null 才能保證 save 做的是「新增」，不會蓋掉別的活動
	    promote.setPromoteNo(null);
	    promote.setPromoteName(promote.getPromoteName().trim());
	    promote.setPromoteBegin(begin.atStartOfDay());
	    promote.setPromoteEnd(end.atTime(23, 59));
	    
	    if(promote.getPromoteContent() != null) {
	        promote.setPromoteContent(promote.getPromoteContent().trim());
	    }
	    
	    if (img != null) {
	        promote.setPromoteImg(img);
	    }
	    return repository.save(promote);
	}

	
	//查詢全部活動(含括分頁跟關鍵字)
	public Page<PromoteVO> getPromoteByStatus(String status, String keyword, LocalDateTime now, int page) {
		status = (status == null) ? "" : status.trim();
		keyword = (keyword == null) ? "" : keyword.trim();
		
		switch (status) {
			case "未開始":
				return repository.findByPromoteNameContainingAndPromoteBeginAfter(keyword, now, getPromotePageable(page));
			case "進行中":
				return repository.findActivePromotes(keyword, now, getPromotePageable(page));
			case "已結束":
				return repository.findByPromoteNameContainingAndPromoteEndBefore(keyword, now, getPromotePageable(page));
			default:
				return repository.findByPromoteNameContaining(keyword, getPromotePageable(page));
		}
	}
	
	//查詢已結束活動（套用已有活動的搜尋用）
	public Page<PromoteVO> getPromoteByEnd(String keyword, LocalDateTime now, int page) {
		keyword = (keyword == null) ? "" : keyword.trim();

		if (keyword.isEmpty()) 
			return Page.empty();
		
		return repository.findByPromoteNameContainingAndPromoteEndBefore(keyword, now, getPromotePageable(page));
	}
	
	//用編號查一筆
	public PromoteVO getPromoteByNo(Integer promoteNo) {
		if (promoteNo == null) {
			return null;
		}
		return repository.findById(promoteNo).orElse(null);
	}
	
	//用編號查一筆活動的圖片
	public byte[] getPromoteImgByNo(Integer promoteNo) {
		PromoteVO promote = getPromoteByNo(promoteNo);   // 編號是 null 或查不到都會拿到 null
		
		if (promote != null) {
			return promote.getPromoteImg();
		}
		return null;
	}
	
	//修改一筆活動
	@Transactional
	public PromoteVO updatePromote(Integer promoteNo, String promoteName, 
									String promoteContent, LocalDateTime promoteBegin, 
									LocalDateTime promoteEnd, byte[] img) {
		PromoteVO promote = getPromoteByNo(promoteNo); 
		if (promote == null) {
			return null; // 找不到該活動，回傳 null
		}

		String status = getStatus(promote);
		if (status.equals("已結束")) {
			return null; // 活動已結束不得修改，回傳 null
		}

		promote.setPromoteName(promoteName.trim());
		promote.setPromoteEnd(promoteEnd.toLocalDate().atTime(23, 59));
		promote.setPromoteContent(promoteContent != null ? promoteContent.trim() : null);

		if (status.equals("未開始")) {
	        promote.setPromoteBegin(promoteBegin.toLocalDate().atStartOfDay());
	    }
		
	    if (img != null) {
	        promote.setPromoteImg(img);
	    }
	    
	    return repository.save(promote);
	}
	
	//刪除一筆活動，保留刪除動作但不接到前端
	@Transactional
	public void deletePromoteByNo(Integer promoteNo) {
		repository.deleteById(promoteNo);
	}
	
	//刪除一筆未開始的活動。不能刪的時候回傳原因（錯誤訊息），刪除成功回傳 null
	@Transactional
	public String deleteNotStartPromote(Integer promoteNo) {
		PromoteVO promote = getPromoteByNo(promoteNo);   // 編號是 null 或查不到都會拿到 null
		if (promote == null) 
			return "查無此活動";
		if (!getStatus(promote).equals("未開始")) 
			return "只有未開始的活動可以刪除";

		if (promote.getOrdersDetails() != null && !promote.getOrdersDetails().isEmpty()) {
			return "這個活動已經有訂單使用，不能刪除";
		}
		repository.delete(promote);
		return null;
	}

	//檢查新的活動，日期是否符合規範，有問題回傳錯誤訊息，沒問題回傳 null
	public String checkNotStartPromoteDate(PromoteVO promote) {
		LocalDate today = LocalDate.now();
		LocalDate begin = promote.getPromoteBegin().toLocalDate();
		LocalDate end = promote.getPromoteEnd().toLocalDate();
		
		if (begin.isBefore(today))
			return "開始日期不能早於今天";
		if (end.isBefore(today)) 
			return "結束日期不能早於今天";
		if (!end.isAfter(begin))
			return "結束日期最早是開始日期的隔天";
		return null;
	}
	
	//檢查修改的活動，日期是否符合規範，有問題回傳錯誤訊息，沒問題回傳 null
	public String checkActivePromoteDate(PromoteVO promote, PromoteVO originalpromote) {
		if (originalpromote == null) {
			return "查無此活動";
		}
		LocalDate today = LocalDate.now();
		LocalDate end = promote.getPromoteEnd().toLocalDate();

		if (end.isBefore(today))
			return "結束日期不能早於今天";

		LocalDateTime originalBegin = originalpromote.getPromoteBegin();
		if (originalBegin != null && !end.isAfter(originalBegin.toLocalDate()))
			return "結束日期最早是開始日期的隔天";
		return null;
	}

	//設定查詢時的分頁每頁幾個
	private static final int PAGE_SIZE = 8;
	
	//設定如何分頁與排序
	private Pageable getPromotePageable(int pageNum) {
		if (pageNum < 1) {
			return PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "promoteEnd","promoteNo"));
		}
		return PageRequest.of(pageNum - 1, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "promoteEnd","promoteNo"));
	}
	

	//判斷一筆活動的狀態
	public String getStatus(PromoteVO promote) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime begin = promote.getPromoteBegin();
	    LocalDateTime end = promote.getPromoteEnd();
	    
	    if (begin != null && now.isBefore(begin)) {
	        return "未開始";
	    }
	    if (end != null && now.isAfter(end)) {
	        return "已結束";
	    }
	    return "進行中";
	}
	

	
}
