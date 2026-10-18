package com.bistroops.feedback.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.bistroops.member.model.MemberVO;
import com.bistroops.orders.model.OrdersService;
import com.bistroops.orders.model.OrdersVO;

@Service
public class FeedbackService {

	@Autowired
	FeedbackRepository feedbackRepository;

	@Autowired
	OrdersService ordersService;
	
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

	// 會員中心的歷史訂單用：把這些訂單的回饋整理成「訂單編號 → 回饋」，沒回饋過的訂單不會在裡面
	public Map<Integer, FeedbackVO> getMapByOrders(List<OrdersVO> orders) {
		if (orders == null || orders.isEmpty()) {
			return Map.of();
		}
		List<Integer> ordersNos = orders.stream().map(OrdersVO::getOrders).toList();
		Map<Integer, FeedbackVO> map = new HashMap<>();
		for (FeedbackVO fb : feedbackRepository.findByOrders_OrdersIn(ordersNos)) {
			map.put(fb.getOrders().getOrders(), fb);
		}
		return map;
	}

	// 這張訂單最晚可以填寫／修改回饋到什麼時候（用餐後 FeedbackVO.FEEDBACK_DAYS 天）
	// HTML 也會用到：${@feedbackService.writeDeadline(order)}（@ 開頭是直接呼叫 Spring 的 Bean）
	public LocalDateTime writeDeadline(OrdersVO order) {
		return order == null || order.getOrdersTime() == null ? null
				: order.getOrdersTime().plusDays(FeedbackVO.FEEDBACK_DAYS);
	}

	// 這張訂單現在還能不能填寫／修改回饋（HTML：${@feedbackService.isWritable(order)}）
	public boolean isWritable(OrdersVO order) {
		LocalDateTime deadline = writeDeadline(order);
		return deadline != null && LocalDateTime.now().isBefore(deadline);
	}

	
	// 前台會員從歷史訂單填寫回饋：用餐後 7 天內，沒回饋過就新增、回饋過就修改
	// 回傳要顯示給會員看的訊息（成功和失敗都是）
	public String saveFromMember(MemberVO member, Integer ordersNo, Integer rating, String content) {
	    String text = content == null ? "" : content.trim();
	    if (member == null) return "請先登入會員";
	    if (ordersNo == null) return "找不到這張訂單";
	    if (rating == null || rating < 1 || rating > 5) return "請選擇 1～5 星的評分";
	    if (text.isEmpty()) return "請填寫回饋內容";
	    if (text.length() > 500) return "回饋內容最多 500 字";

	    // 只能回饋自己的、已結帳的訂單（訂單編號從表單送來，可能被竄改）
	    OrdersVO order = ordersService.getOne(ordersNo);
	    if (order == null || order.getMember() == null || !order.isPaid()
	            || !member.getMemNo().equals(order.getMember().getMemNo())) {
	        return "找不到這張訂單";
	    }

	    FeedbackVO fb = feedbackRepository.findFirstByOrders_Orders(ordersNo);
	    boolean isNew = (fb == null);
	    if (!isWritable(order)) {
	        return "用餐超過 " + FeedbackVO.FEEDBACK_DAYS + " 天，已經不能" + (isNew ? "填寫" : "修改") + "回饋了";
	    }

	    if (isNew) {
	        String name = member.getMemName();
	        fb = new FeedbackVO();
	        fb.setOrders(order);
	        fb.setFeeCustomer(name == null || name.isBlank() ? member.getMemTel() : name); // 沒填姓名用電話代替
	        fb.setFeeCustomerTel(member.getMemTel());
	        fb.setFeeTime(LocalDateTime.now()); // 只在第一次送出時記錄
	    }
	    fb.setFeeRating(rating);
	    fb.setFeeContent(text);
	    feedbackRepository.save(fb);
	    return isNew ? "感謝您的回饋！" : "回饋已更新";
	}
	
	
	//============= 前台非會員從 QR Code 填寫回饋：用餐後 7 天內，沒回饋過就新增=============
	public String saveFromQr(OrdersVO order, String name, String tel, Integer rating, String content) {
        if (order == null) {
                return "QR Code 已失效，請洽服務人員";
        }
        if (!isWritable(order)) {
                return "用餐超過 " + FeedbackVO.FEEDBACK_DAYS + " 天，已經不能填寫回饋了";
        }
        if (feedbackRepository.findFirstByOrders_Orders(order.getOrders()) != null) {
                return "這張訂單已經填寫過回饋了，謝謝您";
        }
        if (rating == null || rating < 1 || rating > 5) {
                return "請選擇 1～5 星的評分";
        }
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) return "請填寫回饋內容";
        if (text.length() > 500) return "回饋內容最多 500 字";

        MemberVO member = order.getMember();
        if (member != null) {
                // 會員：用會員資料，不管表單送什麼
                name = (member.getMemName() == null || member.getMemName().isBlank())
                                ? member.getMemTel() : member.getMemName();
                tel = member.getMemTel();
        } else {
                // 非會員：檢查表單填的
                name = name == null ? "" : name.trim();
                tel = tel == null ? "" : tel.trim();
                if (name.isEmpty()) return "請填寫姓名";
                if (!tel.matches("09\\d{8}")) return "請填寫正確的手機號碼（09 開頭 10 碼）";
        }

        FeedbackVO fb = new FeedbackVO();
        fb.setOrders(order);
        fb.setFeeCustomer(name);
        fb.setFeeCustomerTel(tel);
        fb.setFeeTime(LocalDateTime.now());
        fb.setFeeRating(rating);
        fb.setFeeContent(text);
        feedbackRepository.save(fb);
        return null;
}

	
	
	

}
