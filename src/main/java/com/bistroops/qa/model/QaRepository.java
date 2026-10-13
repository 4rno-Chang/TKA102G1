package com.bistroops.qa.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QaRepository extends JpaRepository<QaVO, Integer> {

	// 點擊次數 +1：直接在資料庫裡加，很多人同時點也不會少算
	@Modifying
	@Query("UPDATE QaVO q SET q.qaClicks = q.qaClicks + 1 WHERE q.qaNo = :qaNo")
	int addClick(@Param("qaNo") Integer qaNo);
}
