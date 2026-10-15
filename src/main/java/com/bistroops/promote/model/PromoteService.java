package com.bistroops.promote.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class PromoteService {
	
	//設定查詢時的分頁每頁幾個結果
	private static final int PAGE_SIZE = 8;

	@Autowired
	private PromoteRepository repository;
	
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
				return repository.findByPromoteNameContainingAndPromoteBeginAfter
											(keyword, now, getPromotePageable(page));
			case "進行中":
				return repository.findActivePromotes(keyword, now, getPromotePageable(page));
			case "已結束":
				return repository.findByPromoteNameContainingAndPromoteEndBefore
											(keyword, now, getPromotePageable(page));
			default:
				return repository.findALLPromoteByName(keyword, getPromotePageable(page));
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
		Optional<PromoteVO> promote = repository.findById(promoteNo);
		
		if(promote.isPresent()) {
			PromoteVO promoteVO = promote.get();
			if(getStatus(promoteVO).equals("已刪除")) {
				return null;
			}
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
	
	//刪除一筆未開始的活動。將狀態改成已刪除，刪除成功回傳 null
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
		repository.cancelPromote(promoteNo);
		return "已刪除活動";
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
	    
	    if (begin == null || end == null) {
	    	return "已刪除";
	    }
	    if (begin != null && now.isBefore(begin)) {
	        return "未開始";
	    }
	    if (end != null && now.isAfter(end)) {
	        return "已結束";
	    }
	    
	    return "進行中";
	}
	
	@Transactional(readOnly = true)
	public Map<Integer, PromoPriceDTO> getActivePrices() {
	    LocalDateTime now = LocalDateTime.now();
	    List<PromoteVO> promotes = repository.findAllActivePromotes(now);
	    
	    Map<Integer, PromoPriceDTO> result = null;
	    
	    result = promotes.stream()
	    				 .flatMap(p -> p.getPromoteMeals().stream())
	    				 .collect(Collectors.toMap(
	    						  pm -> pm.getMeal().getMealNo(),
	    						  pm -> new PromoPriceDTO(
	    							        pm.getPromoteDiscount() != null ? pm.getPromoteDiscount() : pm.getMeal().getMealPrice(),
	    							        pm.getPromote()),  
	    						  (a, b) -> a.getPrice() <= b.getPrice() ? a : b));

	    return result;
	}
	
}
