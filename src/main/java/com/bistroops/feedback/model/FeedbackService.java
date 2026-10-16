package com.bistroops.feedback.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {
	
	@Autowired
	FeedbackRepository feedbackRepository;
	
	  public long count(Integer orders, String tel, LocalDate startDate, LocalDate endDate, Integer rating) {
		  return feedbackRepository.countByCondition(orders, blankToNull(tel),toStart(startDate), toEnd(endDate), rating);
	  }
	
	  public List<FeedbackVO> search(Integer orders, String tel,
              LocalDate startDate, LocalDate endDate, Integer rating,
              int page, int pageSize) {
      // 畫面上的頁數從 1 開始，PageRequest 從 0 開始
      PageRequest pageable = PageRequest.of(Math.max(page, 1) - 1, pageSize);
      return feedbackRepository.searchByCondition(orders, blankToNull(tel),
                      toStart(startDate), toEnd(endDate), rating, pageable);
	  }

	public Double avgRating(Integer orders, String tel,
	              LocalDate startDate, LocalDate endDate, Integer rating) {
	      return feedbackRepository.avgByCondition(orders, blankToNull(tel),
	                      toStart(startDate), toEnd(endDate), rating);
	}
	
	// 空字串 → null（不然會被當成「查電話等於空字串」）
	private String blankToNull(String s) {
	      return (s == null || s.isBlank()) ? null : s.trim();
	}
	
	// LocalDate → LocalDateTime（因為 FEE_TIME 是 DATETIME）
	private LocalDateTime toStart(LocalDate d) {
	      return d == null ? null : d.atStartOfDay();
	}
	
	// 迄日要包含當天，所以取隔天 00:00，查詢用「<」
	private LocalDateTime toEnd(LocalDate d) {
	      return d == null ? null : d.plusDays(1).atStartOfDay();
	}
	public void delete(Integer feedbackNo) {
        feedbackRepository.deleteById(feedbackNo);
	}
	
	
	
	

}
