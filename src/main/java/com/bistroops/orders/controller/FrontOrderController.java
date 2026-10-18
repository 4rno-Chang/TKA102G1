package com.bistroops.orders.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bistroops.meal.model.MenuMealCardDTO;
import com.bistroops.meal.model.MenuService;
import com.bistroops.member.model.MemberService;
import com.bistroops.member.model.MemberVO;
import com.bistroops.orders.model.FrontOrderService;
import com.bistroops.orders.model.FrontSeatService;
import com.bistroops.orders.model.OrderedItemDTO;
import com.bistroops.orderscart.controller.CartWebSocketHandler;
import com.bistroops.orderscart.model.CartItemDTO;
import com.bistroops.orderscart.model.CartRedisService;
import com.bistroops.ordersdetails.controller.WorkboardWebSocketHandler;
import com.bistroops.promote.model.PromoteService;
import com.bistroops.seat.model.SeatVO;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/bistroops")
public class FrontOrderController {
	
	@Autowired
    private FrontSeatService frontSeatService;     // 桌位檢查
	@Autowired
	private MenuService menuService;   // 菜單、購物車內容
	@Autowired
	private FrontOrderService frontOrderService;
    @Autowired
    private CartRedisService cartRedisService;     // 購物車
    @Autowired
    private PromoteService promoteService;         // 活動價
    @Autowired 
    private MemberService memberService;
    @Autowired
    private CartWebSocketHandler cartWebSocketHandler; // 連動同桌其他手機
    @Autowired
    private WorkboardWebSocketHandler workboardWS; // 通知員工的工作面板



    private static final String SEAT_NO = "orderSeatNo";          // session 裡桌號的名字
    private static final String SEATING_KEY = "orderSeatingKey";  // session 裡「這一組的代號」的名字
    private static final int COMMENT_MAX = 30;                     // 備註最多 30 個字
    private static final String BACK = "redirect:/bistroops/order";
	
	//進入頁面
	@GetMapping("/order")
	public String order(@RequestParam(required = false) String seat,
	                    @RequestParam(required = false) String open,
	                    HttpSession session, Model model) {
		//拿桌號
	    if (seat != null && !seat.isBlank()) {
	        SeatVO scanned = frontSeatService.getSeat(seat.trim());
	        String error = frontSeatService.checkSeat(scanned, null);// 剛掃的人用
	        if (error != null) {
	            model.addAttribute("seatNo", seat.trim());
	            model.addAttribute("orderError", error);
	            return "front/order/order";
	        }
	        session.setAttribute(SEAT_NO, scanned.getSeatNo());
	        session.setAttribute(SEATING_KEY, frontSeatService.getSeatingKey(scanned));
	        return BACK;// 把網址上的 ?seat= 拿掉
	    }

	    // 重新整理、redirect 回來，桌號從 session 拿
	    String seatNo = (String) session.getAttribute(SEAT_NO);
	    if (seatNo == null) {
	        return "front/order/order";                // 沒有 seatNo → 頁面顯示「請掃描桌上的 QR code」
	    }
	    model.addAttribute("seatNo", seatNo);

	    SeatVO s = frontSeatService.getSeat(seatNo);
	    String error = frontSeatService.checkSeat(s, (String) session.getAttribute(SEATING_KEY));
	    if (error != null) {
	        if (FrontSeatService.MSG_CHANGED.equals(error)) {
	            clearSeat(session);
	        }
	        model.addAttribute("orderError", error);   // 頁面只顯示原因，菜單和購物車都不出現
	        return "front/order/order";
	    }

	    //菜單(活動價只查一次，菜單和購物車共用)
	    List<MenuMealCardDTO> cards = menuService.getMenuCards(promoteService.getActivePrices());
	    model.addAttribute("menuSections", menuService.getMenuSections(cards));
	    model.addAttribute("promoMeals", menuService.getPromoMeals(cards));

	    // 加入購物車
	    List<CartItemDTO> cartItems = frontOrderService.getCartItems(frontSeatService.getSeatingKey(s), cards);
	    model.addAttribute("cartItems", cartItems);
	    model.addAttribute("cartCount", cartItems.stream().mapToInt(CartItemDTO::getNum).sum());
	    model.addAttribute("cartTotal", cartItems.stream().mapToInt(CartItemDTO::getSubtotal).sum());

	    //加入已點餐點
	    List<OrderedItemDTO> orderedItems = frontOrderService.getOrderedItems(s);
	    model.addAttribute("orderedItems", orderedItems);
	    model.addAttribute("orderedTotal", orderedItems.stream()
	            .filter(d -> !d.isCancelled())
	            .mapToInt(OrderedItemDTO::getSubtotal)
	            .sum());

	    model.addAttribute("open", open);               // "cart" / "ordered"：一進頁面就打開那個面板
	    return "front/order/order";
	}
	
	@PostMapping("/order/cart/add")
	public String cartAdd(@RequestParam(required = false) Integer mealNo,
	                      @RequestParam(required = false) Integer num,
	                      @RequestParam(required = false) String comment,
	                      HttpSession session, RedirectAttributes ra) {
	    SeatVO seat = checkedSeat(session, ra);
	    if (seat == null) {
	        return BACK;
	    }
	    // 數量要是 1～10(設定上超過10要去跟客人確認是否重複下單，如果客人必須要，那就改備註再繼續點)
	    if (num == null || num < 1 || num > CartRedisService.MAX_NUM) {
	        ra.addFlashAttribute("message", "數量要是 1～" + CartRedisService.MAX_NUM + " 份。");
	        return BACK;
	    }
	    // 菜要存在、而且是上架的
	    MenuMealCardDTO card = findCard(mealNo);
	    if (card == null) {
	        ra.addFlashAttribute("message", "這道菜目前沒有供應。");
	        return BACK;
	    }

	    cartRedisService.add(frontSeatService.getSeatingKey(seat), mealNo, num, cleanComment(comment));
	    cartWebSocketHandler.notifyCartChanged(frontSeatService.getSeatingKey(seat));

	    ra.addFlashAttribute("message", "已加入購物車：" + card.getMealName() + " × " + num);
	    return BACK + "#meal-" + mealNo;             // # 後面是那道菜的 id，回來時瀏覽器會捲到那裡
	}
	
	@PostMapping("/order/cart/update")
	public String cartUpdate(@RequestParam(required = false) Integer mealNo,
	                         @RequestParam(required = false) String oldComment,
	                         @RequestParam(required = false) Integer num,
	                         @RequestParam(required = false) String comment,
	                         HttpSession session, RedirectAttributes ra) {
	    SeatVO seat = checkedSeat(session, ra);
	    if (seat == null) {
	        return BACK;
	    }
	    if (mealNo != null && num != null) {
	        // num 是 0 以下 → 移除；超過 10 → 10（都在 CartRedisService.update 裡處理）
	        cartRedisService.update(frontSeatService.getSeatingKey(seat), mealNo,
	                                cleanComment(oldComment), num, cleanComment(comment));
	        cartWebSocketHandler.notifyCartChanged(frontSeatService.getSeatingKey(seat));
	    }
	    return BACK + "?open=cart";                  // 回來之後購物車還是開著
	}
	
	//從購物車移除明細
	@PostMapping("/order/cart/remove")
	public String cartRemove(@RequestParam(required = false) Integer mealNo,
	                         @RequestParam(required = false) String oldComment,
	                         HttpSession session, RedirectAttributes ra) {
	    SeatVO seat = checkedSeat(session, ra);
	    if (seat == null) {
	        return BACK;
	    }
	    if (mealNo != null) {
	        cartRedisService.remove(frontSeatService.getSeatingKey(seat), mealNo, cleanComment(oldComment));
	        cartWebSocketHandler.notifyCartChanged(frontSeatService.getSeatingKey(seat));
	    }
	    return BACK + "?open=cart";
	}
	
	//清空購物車
	@PostMapping("/order/cart/clear")
	public String cartClear(HttpSession session, RedirectAttributes ra) {
	    SeatVO seat = checkedSeat(session, ra);
	    if (seat == null) {
	        return BACK;
	    }
	    cartRedisService.clear(frontSeatService.getSeatingKey(seat));
	    cartWebSocketHandler.notifyCartChanged(frontSeatService.getSeatingKey(seat));
	    return BACK;
	}
	
	//下單
	@PostMapping("/order/submit")
	public String submit(HttpSession session, RedirectAttributes ra) {
	    SeatVO seat = checkedSeat(session, ra);
	    if (seat == null) {
	        return BACK;
	    }
	    
	    Integer memNo = (Integer) session.getAttribute("memberNo");    // 沒有登入是 null
	    MemberVO member = (memNo == null) ? null : memberService.getMemNoQuery(memNo);
	    String error = frontOrderService.submitOrder(seat, member);
	    
	    if (error != null) {
	        ra.addFlashAttribute("message", error);
	        return BACK + "?open=cart";
	    }
	    
	    cartWebSocketHandler.notifyCartChanged(frontSeatService.getSeatingKey(seat));
	    workboardWS.notifyChanged();
	    ra.addFlashAttribute("message", "訂單已送出，餐點製作中");
	    return BACK;
	}

	
	//單獨讀出圖片
	@GetMapping("/order/mealImg")
	public ResponseEntity<byte[]> mealImg(@RequestParam(required = false) Integer mealNo) {
	    byte[] img = (mealNo != null) ? menuService.getMealImg(mealNo) : null;
	    if (img == null || img.length == 0) {
	        return ResponseEntity.notFound().build();                   // 404：頁面會改顯示預設插圖
	    }
	    return ResponseEntity.ok()
	            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))   // 1 小時內瀏覽器不重新下載同一張
	            .contentType(MediaType.IMAGE_JPEG)
	            .body(img);
	}

	

	
	// 從 session 找出這支手機的桌位，並檢查還能不能點餐。可以 → 回傳桌位；不行 → 回傳 null
	private SeatVO checkedSeat(HttpSession session, RedirectAttributes ra) {
	    String seatNo = (String) session.getAttribute(SEAT_NO);
	    if (seatNo == null) {
	        ra.addFlashAttribute("message", "請掃描桌上的 QR code。");
	        return null;
	    }
	    SeatVO seat = frontSeatService.getSeat(seatNo);
	    String error = frontSeatService.checkSeat(seat, (String) session.getAttribute(SEATING_KEY));
	    if (error != null) {
	        if (FrontSeatService.MSG_CHANGED.equals(error)) {
	            clearSeat(session);              // 換了客人：這支手機要重新掃 QR code
	        }
	        ra.addFlashAttribute("message", error);
	        return null;
	    }
	    return seat;
	}
	
	// 把 session 裡的桌號和代號拿掉
	private void clearSeat(HttpSession session) {
	    session.removeAttribute(SEAT_NO);
	    session.removeAttribute(SEATING_KEY);
	}

	// 備註整理：null 當作沒有、去掉前後空白、超過 30 個字只留前 30 個
	private String cleanComment(String comment) {
	    if (comment == null) {
	        return "";
	    }
	    String c = comment.trim();
	    return (c.length() > COMMENT_MAX) ? c.substring(0, COMMENT_MAX) : c;
	}

	// 用菜品編號找「上架」的那道菜（找不到 = 不存在或下架）
	private MenuMealCardDTO findCard(Integer mealNo) {
	    if (mealNo == null) {
	        return null;
	    }
	    return menuService.getMenuCards(Map.of()).stream()
	            .filter(c -> c.getMealNo().equals(mealNo))
	            .findFirst()
	            .orElse(null);
	}

}
