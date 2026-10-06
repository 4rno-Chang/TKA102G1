package com.bistroops.announcement.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<AnnouncementVO, Integer> {

	// 前台用：WHERE ann_begin <= ? ORDER BY ann_begin DESC
	List<AnnouncementVO> findByAnnBeginLessThanEqualOrderByAnnBeginDesc(LocalDateTime now);

}
