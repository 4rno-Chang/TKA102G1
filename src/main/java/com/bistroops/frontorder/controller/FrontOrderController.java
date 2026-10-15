package com.bistroops.frontorder.controller;

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

import com.bistroops.frontorder.model.CartItemDTO;
import com.bistroops.frontorder.model.CartRedisService;
import com.bistroops.frontorder.model.FrontOrderService;
import com.bistroops.frontorder.model.FrontSeatService;
import com.bistroops.frontorder.model.MenuMealCardDTO;
import com.bistroops.frontorder.model.OrderedItemDTO;
import com.bistroops.member.model.MemberVO;
import com.bistroops.promote.model.PromoteService;
import com.bistroops.seat.model.SeatVO;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/bistroops")
public class FrontOrderController {

	// ══════════════════════════════════════════════════════════════════════════════
	// 前台「線上點餐」（桌邊點餐）：客人用手機掃桌上的 QR code 進來點餐的頁面
	//   頁面：templates/front/order/order.html　　CSS：static/css/front/order.css　　JS：static/js/front/order.js
	//   網址：/bistroops/order?seat=A01      ← QR code 裡放的網址，seat 是桌號
	//   已經寫好、測試過：步驟 1～6 全部（打開頁面、四個改購物車的方法、送出訂單、菜的圖片、小幫手）。
	// ══════════════════════════════════════════════════════════════════════════════
	//
	// ══ ★ 還沒做的清單 ══
	//   [✓] 8. 這個檔案：步驟 4 submit（送出訂單）、步驟 6 mealImg（菜的圖片）（已經寫好、測試過了）
	//   [✓] 9. pom.xml                                 加 spring-boot-starter-websocket（已經加好了）
	//   [✓] 10. frontorder/config/CartWebSocketConfig.java     WebSocket 的設定（已經寫好、測試過了）
	//   [✓] 11. frontorder/controller/CartWebSocketHandler.java  WebSocket 的分組和通知（已經寫好、測試過了）
	//   [✓] 11-1. 這個檔案：注入 CartWebSocketHandler，在 cartAdd、cartUpdate、cartRemove、cartClear、submit
	//             改完購物車之後呼叫 notifyCartChanged（已經寫好、測試過了：成功才通知，被擋下或失敗不通知）
	//   [✓] 12. static/js/front/order.js 的 A-7        已經啟用（打開頁面就會連 /bistroops/order/ws）
	//   [✓] 13. 假資料已經刪掉了（order.html 的 5 段、order.js 的 B 區、小標籤、order.css 的第 10 區、<body> 的桌號屬性）
	//   ★ FrontSeatService 的 DINING_MINUTES 現在是測試用的 10，正式版改回 90。
	//   8 做完，點餐就可以用了（只是同桌的手機要重新整理才看得到彼此加的東西）；9～12 是讓同桌的手機購物車互通。
	//
	// ══ [✓] 步驟 4：送出訂單（已經寫好、測試過了）══
	//   @PostMapping("/order/submit")，參數：HttpSession session, RedirectAttributes ra（表單沒有任何欄位）
	//     SeatVO seat = checkedSeat(session, ra);  是 null → return BACK;
	//     MemberVO member = (MemberVO) session.getAttribute("member");      ← 沒有登入是 null
	//     String error = frontOrderService.submitOrder(seat, member);
	//     有 error → ra.addFlashAttribute("message", error); return BACK + "?open=cart";
	//     成功    → ra.addFlashAttribute("message", "訂單已送出，餐點製作中"); return BACK + "?open=ordered";
	//   import：com.bistroops.member.model.MemberVO
	//
	// ══ [✓] 步驟 6：菜的圖片（已經寫好、測試過了）══
	//   @GetMapping("/order/mealImg")，參數：@RequestParam(required = false) Integer mealNo，回傳 ResponseEntity<byte[]>
	//     byte[] img = (mealNo != null) ? frontOrderService.getMealImg(mealNo) : null;
	//     img 是 null 或長度 0 → return ResponseEntity.notFound().build();      ← 404，頁面會改顯示預設插圖
	//     return ResponseEntity.ok()
	//             .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))   // 1 小時內同一張圖不再重新下載
	//             .contentType(MediaType.IMAGE_JPEG)
	//             .body(img);
	//   import：org.springframework.http.CacheControl、MediaType、ResponseEntity、java.util.concurrent.TimeUnit
	//   這個方法不用檢查桌號（菜的照片不是秘密，菜單頁每張圖都會來要一次）。
	//
	// ══ [✓] WebSocket：同桌的手機購物車互通（9～12，已經完成）══
	//   A 手機加了東西，B 手機要重新整理才看得到；WebSocket 讓伺服器「主動」通知 B 手機。
	//   做法（不用 JSON）：伺服器只傳一句「cart-changed」給同一組的其他手機，手機收到就重新載入頁面。
	//     1. pom.xml 加（★ 大家共用的檔案，先跟組員說；加完 Eclipse 對專案按右鍵 → Maven → Update Project）：
	//          <dependency>
	//              <groupId>org.springframework.boot</groupId>
	//              <artifactId>spring-boot-starter-websocket</artifactId>
	//          </dependency>
	//     2. config/CartWebSocketConfig.java：「/bistroops/order/ws 是 WebSocket」，並把 HTTP 的 session 帶過去
	//        （要靠 session 裡的 orderSeatingKey 知道這支手機是哪一組客人）。
	//     3. controller/CartWebSocketHandler.java：依「這一組的代號」分群組；提供 notifyCartChanged(代號)。
	//     4. 這個 Controller：@Autowired private CartWebSocketHandler cartWebSocketHandler;
	//        在 cartAdd、cartUpdate、cartRemove、cartClear、submit 改完購物車之後，
	//        呼叫 cartWebSocketHandler.notifyCartChanged(frontSeatService.getSeatingKey(seat));
	//     5. order.js 的 A-7：刪掉兩行註解記號就會啟用（收到 cart-changed 就重新載入；對話框開著時改成顯示提示）。
	//   ★ 測試：同一台電腦開兩個不同的瀏覽器（例如 Chrome 和 Edge），打開同一桌，一邊加東西，另一邊應該會自己更新。
	//     用手機測的話用電腦的 IP（命令提示字元打 ipconfig），不要用 localhost。
	//
	// ══ [✓] 全部做完之後（已經刪掉假資料）══
	//   確認資料庫的畫面沒問題後刪掉假資料：
	//     ・order.html：用 ▼▼▼【假資料・刪除範圍 開始】到 ▲▲▲【假資料・刪除範圍 結束】框起來的 5 段
	//     ・order.js：「B. 假資料」整區（▼▼▼ 到 ▲▲▲；最後一行的 })(); 要留著）
	//     ・（可以一起刪）order.html 的小標籤 source-line、source-tag，order.css 的第 10 區（小標籤的樣式），
	//       以及 <body> 上的 th:attr="data-seat=..."（只有假資料用到）
	//   ★ order.js 的 A 區（A-1～A-7）不要刪：那是資料庫的版本也會用到的程式。

	// 步驟 1、2、3、5：打開點餐頁
	
	@Autowired
    private FrontSeatService frontSeatService;     // 桌位檢查
    @Autowired
    private FrontOrderService frontOrderService;   // 菜單、購物車內容
    @Autowired
    private CartRedisService cartRedisService;     // 購物車
    @Autowired
    private PromoteService promoteService;         // 活動價
    @Autowired
    private CartWebSocketHandler cartWebSocketHandler; // 連動同桌其他手機


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
	    List<MenuMealCardDTO> cards = frontOrderService.getMenuCards(promoteService.getActivePrices());
	    model.addAttribute("menuSections", frontOrderService.getMenuSections(cards));
	    model.addAttribute("promoMeals", frontOrderService.getPromoMeals(cards));

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
	    // 數量要是 1～20(設定上超過20要去跟客人確認是否重複下單，如果客人必須要，那就改備註再繼續點)
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
	        // num 是 0 以下 → 移除；超過 20 → 20（都在 CartRedisService.update 裡處理）
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
	    MemberVO member = (MemberVO) session.getAttribute("member");   // 沒有登入是 null
	    String error = frontOrderService.submitOrder(seat, member);
	    if (error != null) {
	        ra.addFlashAttribute("message", error);
	        return BACK + "?open=cart";
	    }
	    cartWebSocketHandler.notifyCartChanged(frontSeatService.getSeatingKey(seat));
	    ra.addFlashAttribute("message", "訂單已送出，餐點製作中");
	    return BACK + "?open=ordered";               // 回來之後直接打開「已點餐點」
	}

	
	//單獨讀出圖片
	@GetMapping("/order/mealImg")
	public ResponseEntity<byte[]> mealImg(@RequestParam(required = false) Integer mealNo) {
	    byte[] img = (mealNo != null) ? frontOrderService.getMealImg(mealNo) : null;
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
	    return frontOrderService.getMenuCards(Map.of()).stream()
	            .filter(c -> c.getMealNo().equals(mealNo))
	            .findFirst()
	            .orElse(null);
	}

}
