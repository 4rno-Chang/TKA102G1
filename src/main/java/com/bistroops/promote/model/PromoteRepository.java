package com.bistroops.promote.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;



@Repository
public interface PromoteRepository extends JpaRepository<PromoteVO, Integer> {

	//查詢活動有關鍵字的全部活動，依進行中、未開始、已結束分組；同一組裡開始時間離現在近的在前（未開始：開始早的在前；其他：開始晚的在前）
	@Query("SELECT p FROM PromoteVO p WHERE " +
	       "(:keyword = '' OR p.promoteName LIKE CONCAT('%', :keyword, '%')) AND " +
	       "p.promoteBegin IS NOT NULL AND p.promoteEnd IS NOT NULL " +
	       "ORDER BY CASE WHEN p.promoteBegin <= :now AND p.promoteEnd >= :now THEN 0 " +   
	       "              WHEN p.promoteBegin > :now THEN 1 " + 
	       "              ELSE 2 END, " +
	       "         CASE WHEN p.promoteBegin > :now THEN p.promoteBegin END ASC, "+
		   "         p.promoteBegin DESC, p.promoteNo DESC")
	Page<PromoteVO> findALLPromoteByName(@Param("keyword") String promoteName,
	                                     @Param("now") LocalDateTime now, Pageable pageable);
	

	//查詢活動有關鍵字且未開始的活動含分頁
	Page<PromoteVO> findByPromoteNameContainingAndPromoteBeginAfter(
			String keyword, LocalDateTime now, Pageable pageable);
	
	//查詢活動有關鍵字且進行中的活動含分頁
	@Query("SELECT p FROM PromoteVO p WHERE " +
	       "(:keyword = '' OR p.promoteName LIKE CONCAT('%', :keyword, '%')) AND " +
	       "p.promoteBegin <= :now AND p.promoteEnd >= :now")
	Page<PromoteVO> findActivePromotes(
	            @Param("keyword") String keyword, 
	            @Param("now") LocalDateTime now, 
	            Pageable pageable);
	
	//查詢所有進行中的活動
	@Query("SELECT p FROM PromoteVO p WHERE " +
		       "p.promoteBegin <= :now AND p.promoteEnd >= :now")
	List<PromoteVO> findAllActivePromotes(
				@Param("now") LocalDateTime now);

	//查詢活動有關鍵字已結束的活動含分頁
	Page<PromoteVO> findByPromoteNameContainingAndPromoteEndBefore(
					String keyword, LocalDateTime now, Pageable pageable);
	
	@Transactional
	@Modifying
	@Query("UPDATE PromoteVO p SET p.promoteBegin = null, p.promoteEnd = null WHERE p.promoteNo = :promoteNo")
	public int cancelPromote(@Param("promoteNo") Integer promoteNo);

	//菜單管理的對話框：查詢還沒結束的活動，不帶圖片跟分頁，開始早的排前面
	//已刪除的活動（cancelPromote 把時間改成 null）不會查到：null 跟任何時間比都不成立
	@Query("SELECT p.promoteNo AS promoteNo, p.promoteName AS promoteName, " +
	       "p.promoteBegin AS promoteBegin, p.promoteEnd AS promoteEnd " +
	       "FROM PromoteVO p WHERE p.promoteEnd >= :now ORDER BY p.promoteBegin")
	List<PromoteOptionDTO> findOpenOptions(@Param("now") LocalDateTime now);

	// 結帳「加入整單折扣」對話框的選項：進行中、有設定整單折數的活動，折越多排越前面
	@Query("SELECT p.promoteNo AS promoteNo, p.promoteName AS promoteName, "+
			"p.promoteDisc AS promoteDisc, p.promoteRule AS promoteRule " +
           "FROM PromoteVO p WHERE p.promoteDisc IS NOT NULL " +
           "AND p.promoteBegin <= :now AND p.promoteEnd >= :now "+
           "ORDER BY p.promoteDisc")
	List<OrderPromoteOptionDTO> findPromoteOptions(@Param("now") LocalDateTime now);

	// 無條件自動套用要挑的活動：進行中、有整單折數、沒有條件；折數最多的排第一，一樣多就比較早結束的排前面（取第一筆用）
	@Query("SELECT p.promoteNo AS promoteNo, p.promoteName AS promoteName, " +
		   "p.promoteDisc AS promoteDisc, p.promoteRule AS promoteRule " +
		   "FROM PromoteVO p WHERE p.promoteDisc IS NOT NULL AND p.promoteRule IS NULL " +
		   "AND p.promoteBegin <= :now AND p.promoteEnd >= :now " +
		   "ORDER BY p.promoteDisc ASC, p.promoteEnd ASC")
	List<OrderPromoteOptionDTO> findAutoApplyPromotes(@Param("now") LocalDateTime now);

	//用活動編號查名稱、條件、折數（結帳頁「整單折扣」那一行顯示用；不加日期條件，活動結束了訂單還是要顯示名稱）
	//這裡的折數是活動「現在」的折數，訂單要顯示的是快照 orders_promote_disc
	@Query("SELECT p.promoteNo AS promoteNo, p.promoteName AS promoteName, " +
	       "p.promoteDisc AS promoteDisc, p.promoteRule AS promoteRule " +
	       "FROM PromoteVO p WHERE p.promoteNo = :promoteNo")
	OrderPromoteOptionDTO findDiscInfoByNo(@Param("promoteNo") Integer promoteNo);
	
}
