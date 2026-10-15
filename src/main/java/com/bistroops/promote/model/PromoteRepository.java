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

	//查詢活動有關鍵字的全部活動
	@Query("SELECT p FROM PromoteVO p WHERE " +
		   "(:keyword = '' OR p.promoteName LIKE CONCAT('%', :keyword, '%')) AND " +
		   "p.promoteBegin IS NOT NULL AND p.promoteEnd IS NOT NULL")
	Page<PromoteVO> findALLPromoteByName(
			@Param("keyword") String promoteName, Pageable pageable);
	

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
	

}
