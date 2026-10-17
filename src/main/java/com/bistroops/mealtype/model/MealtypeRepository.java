package com.bistroops.mealtype.model;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MealtypeRepository extends JpaRepository<MealtypeVO, Integer> {
	
	//類別查詢
	@Query("SELECT t FROM MealtypeVO t WHERE t.mealtypeHashtag IS NOT NULL ORDER BY t.mealtypeNo")
	List<MealtypeVO> findInUse(); 

}
