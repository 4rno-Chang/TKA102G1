package com.bistroops.promote.model;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/*
 * 活動（資料表 promote）的 Repository：負責「去資料庫查、存」。
 *
 * ── 活動管理的說明放在哪裡（每件事只寫在一個地方）──
 *   PromoteController.java   有哪些網址、每個方法收什麼參數、放什麼進 Model、回傳哪一頁（串接從那裡開始看）
 *   PromoteService.java      規則：日期與時間、能不能修改、圖片怎麼存；還沒寫的功能清單
 *   PromoteRepository.java   （這個檔案）查詢怎麼寫；★ 之後的優化「列表不讀圖片（回傳小物件）」
 *   promote_list.html、promote_form.html   畫面用到哪些 Model 的名稱和欄位、假資料要刪哪裡
 *   promote.js、promote.css  畫面上的檢查、日期選擇、圖片預覽、樣式
 *
 * 【目前的狀態】（實際把這個介面和所有 VO 載入檢查過；沒有連資料庫，所以沒有真的查）
 *   ✔ 列表頁需要的四個查詢都寫好了，名稱和 PromoteVO 的屬性對得上，自己寫的 @Query 語法也正確。
 *   ✔ 繼承 JpaRepository 就已經有：用編號查一筆（findById）、存一筆（save，新增和修改都是它）。
 *   所以串接列表、詳細、新增、修改、圖片，這個檔案都「不用再加東西」。
 *   之後要動這個檔案的只有最下面的「之後的優化」。
 *
 * 【四個查詢】每一個都同時做三件事：活動名稱裡有關鍵字、符合狀態、分頁加排序。
 *   活動狀態「不存在資料表」，是用時間比出來的：
 *     全部    只看活動名稱
 *     未開始  開始時間 晚於 現在
 *     進行中  開始時間 不晚於 現在，而且 結束時間 不早於 現在
 *     已結束  結束時間 早於 現在
 *   ・「現在」由呼叫的人（PromoteService）傳進來，不要寫死在查詢裡。
 *   ・比的是「完整的時間」。存進去的結束時間一律是那一天的 23:59，所以活動在結束日期當天整天都算進行中，
 *     隔天才會被「已結束」查到。
 *   ・分頁和排序由最後一個參數 Pageable 決定（PromoteService 的 getPromotePageable：一頁 8 筆，
 *     結束時間晚的排前面，同時間再比活動編號）。回傳的 Page 裡有這一頁的資料，也有總頁數、總筆數。
 *
 * 【要注意】
 *   ・★ 關鍵字不能傳 null：不會出錯，但四個查詢都會「一筆都查不到」。沒有輸入關鍵字時要傳空字串 ""。
 *   ・兩種寫法都可以：「進行中」是自己寫 @Query，另外三個是用方法名稱自動產生。
 *     方法名稱的寫法：照 Spring Data 的規則取名（Containing 裡面有這些字、After 晚於、Before 早於、And 而且），
 *     名稱裡寫的是 PromoteVO 的「屬性名稱」，拼錯一個字程式啟動時就會出錯。
 *     @Query 的寫法：寫的是 VO 的類別名稱和屬性名稱（PromoteVO、p.promoteName），不是資料表和欄位的名稱。
 *   ・不要用「查全部」（findAll）來做列表：活動一多，每次都會把所有資料連同圖片讀出來。
 *   ・「套用已有活動」的搜尋用的也是「已結束」那一個查詢，不用另外寫（這個功能最後再做）。
 *   ・首頁的輪播之後要顯示「進行中、而且有圖片」的活動，到時候再加一個查詢（條件多一個：圖片不是空的）。
 *
 *
 * ════════════════════════════════════════════════════════════════════════════
 * ★【之後的優化，可以不做】列表的查詢不要把圖片讀出來（回傳「小物件」）
 * ════════════════════════════════════════════════════════════════════════════
 *   什麼時候做：查詢、詳細、新增、修改、圖片都串接好、確認沒問題之後。不做也能正常使用，只是比較慢。
 *
 *   現在的情況：
 *     四個查詢回傳的是完整的 PromoteVO，所以每一筆都會把圖片（promoteImg）從資料庫讀出來，一頁最多 8 張
 *     （前端限制一張 5MB，最壞的情況一次大約 40MB）。
 *     但列表頁只用它判斷「有沒有圖片」；真正顯示圖片時，瀏覽器會另外連輸出圖片的網址，那時又讀一次。
 *     等於每張圖被讀了兩次，第一次是白讀的。
 *   目標：
 *     列表只查卡片需要的文字欄位，再加一個「有沒有圖片」（true 或 false），不把圖片的內容讀出來。
 *   老師說的「圖片要另外處理」如果是指「圖片要有自己的網址輸出、不要跟著頁面一起送」，現在已經做到了；
 *   如果是指「查列表時不要讀圖片」，就是這一段。
 *
 *   做法（Spring Data 的 projection，中文常叫「投影」）。要改五個地方：
 *
 *   1. 新增一個「小物件」的介面（interface），放在這個 model 資料夾。
 *      名稱自己取，下面用 PromoteCard 當例子。裡面只寫卡片需要的 getter：
 *          Integer getPromoteNo();
 *          String getPromoteName();
 *          LocalDateTime getPromoteBegin();
 *          LocalDateTime getPromoteEnd();
 *          Boolean getHasImg();        ← 有沒有圖片。PromoteVO 沒有這個，是查詢時算出來的（見第 2 點）
 *      只要寫介面，不用寫實作它的類別，Spring Data 會自己產生。
 *
 *   2. 這個檔案的四個查詢：回傳型別從 Page<PromoteVO> 改成 Page<PromoteCard>，而且四個都改用 @Query。
 *      原因：「有沒有圖片」要用 CASE WHEN 算出來，方法名稱的寫法做不到。以「已結束」為例：
 *          @Query(value = "SELECT p.promoteNo AS promoteNo, p.promoteName AS promoteName, "
 *                       + "p.promoteBegin AS promoteBegin, p.promoteEnd AS promoteEnd, "
 *                       + "CASE WHEN p.promoteImg IS NOT NULL THEN true ELSE false END AS hasImg "
 *                       + "FROM PromoteVO p "
 *                       + "WHERE p.promoteName LIKE CONCAT('%', :keyword, '%') AND p.promoteEnd < :now",
 *                 countQuery = "SELECT COUNT(p) FROM PromoteVO p "
 *                       + "WHERE p.promoteName LIKE CONCAT('%', :keyword, '%') AND p.promoteEnd < :now")
 *          Page<PromoteCard> 方法名稱(@Param("keyword") String keyword, @Param("now") LocalDateTime now, Pageable pageable);
 *      重點：
 *        ・AS 後面的名字要和介面的 getter 對得上（AS hasImg 對 getHasImg()）。對不上的話，那個欄位會拿到 null。
 *        ・SELECT 裡沒有寫 p.promoteImg，所以圖片的內容不會被讀出來；IS NOT NULL 只是問資料庫「有沒有」。
 *        ・countQuery 是「算總共幾筆」用的查詢（分頁要知道總頁數），它的 WHERE 要和上面那一個一樣。
 *        ・另外三個查詢只有 WHERE 不一樣（條件見上面的【四個查詢】）。
 *        ・排序和分頁一樣交給 Pageable，查詢裡不用寫 ORDER BY。
 *        ・字串用 + 接起來的時候，每一段的最後要留一個空白，不然前後兩個字會黏在一起變成語法錯誤。
 *      這個寫法在專案外面實際試過，Spring Data 可以接受（沒有連資料庫，所以沒有真的查）。
 *
 *   3. PromoteService：四個查詢方法的回傳型別跟著改成 Page<PromoteCard>，其他不用動。
 *
 *   4. PromoteController：步驟 1 的 Page<PromoteVO> 換成 Page<PromoteCard>，其他不用動
 *      （它只是把 result.getContent() 放進 Model，裡面裝的東西從 PromoteVO 變成 PromoteCard）。
 *
 *   5. promote_list.html：卡片上判斷有沒有圖片的兩行要改（那裡有 ★ 標記）：
 *          p.promoteImg != null  改成  p.hasImg
 *          p.promoteImg == null  改成  !p.hasImg
 *      其他的 p.promoteNo、p.promoteName、p.promoteBegin、p.promoteEnd 都不用改。
 *
 *   不用改的地方：
 *     ・「用編號查一筆」（活動詳細、修改的表單、輸出圖片）維持查完整的 PromoteVO：一次只有一筆，而且真的需要圖片。
 *     ・存檔一定要用完整的 PromoteVO。小物件只能拿來顯示，不能拿來存。
 *     ・「套用已有活動」的搜尋結果只需要名稱和結束時間，之後也可以用同一個小物件。
 *   不建議的做法：在 PromoteVO 的 promoteImg 上加 @Basic(fetch = FetchType.LAZY)。
 *     看起來最簡單，但 Hibernate 要另外設定才會生效，沒設定的話會被忽略，圖片照樣讀出來。
 */
@Repository
public interface PromoteRepository extends JpaRepository<PromoteVO, Integer> {
	//查詢活動有關鍵字的全部活動
	Page<PromoteVO> findByPromoteNameContaining(String promoteName, Pageable pageable);
	

	//查詢活動有關鍵字且未開始的活動
	Page<PromoteVO> findByPromoteNameContainingAndPromoteBeginAfter(
			String keyword, LocalDateTime now, Pageable pageable);
	
	//查詢活動有關鍵字且進行中的活動
	@Query("SELECT p FROM PromoteVO p WHERE " +
	       "(:keyword = '' OR p.promoteName LIKE CONCAT('%', :keyword, '%')) AND " +
	       "p.promoteBegin <= :now AND p.promoteEnd >= :now")
	Page<PromoteVO> findActivePromotes(
	            @Param("keyword") String keyword, 
	            @Param("now") LocalDateTime now, 
	            Pageable pageable);

	//查詢活動有關鍵字已結束的活動
	Page<PromoteVO> findByPromoteNameContainingAndPromoteEndBefore(
					String keyword, LocalDateTime now, Pageable pageable);

}
