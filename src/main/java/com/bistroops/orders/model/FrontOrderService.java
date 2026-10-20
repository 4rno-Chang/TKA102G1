package com.bistroops.orders.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bistroops.meal.model.MenuMealCardDTO;
import com.bistroops.meal.model.MenuRepository;
import com.bistroops.meal.model.MenuService;
import com.bistroops.member.model.MemberVO;
import com.bistroops.orderscart.model.CartItemDTO;
import com.bistroops.orderscart.model.CartLineDTO;
import com.bistroops.orderscart.controller.CartWebSocketHandler;
import com.bistroops.orderscart.model.CartRedisService;
import com.bistroops.ordersdetails.model.OrdersDetailsRepository;
import com.bistroops.ordersdetails.model.OrdersDetailsVO;
import com.bistroops.promote.model.OrderPromoteOptionDTO;
import com.bistroops.promote.model.PromoPriceDTO;
import com.bistroops.promote.model.PromoteService;
import com.bistroops.seat.model.SeatVO;

@Service
public class FrontOrderService {

	@Autowired
	private OrderedItemRepository orderedItemRepository;
	@Autowired
	private OrdersRepository ordersRepository;
	@Autowired
	private OrdersDetailsRepository ordersDetailsRepository;
	@Autowired
	private MenuRepository menuRepository;
	@Autowired
	private OrdersService ordersSve;
	@Autowired
	private CartRedisService cartRedisSve;
	@Autowired
	private PromoteService promoteSve;
	@Autowired
	private FrontSeatService frontSeatService;
	@Autowired
	private MenuService menuSve;
	@Autowired
	private CartWebSocketHandler cartWebSocketHandler;   // 通知同一桌客人的手機（WebSocket）

	// 為了讓客人手機的「已點餐點」跟工作面板同步：工作面板改了已送出的訂單之後，通知那一桌客人的手機
	//   什麼時候呼叫：OrdersDetailsController 的「儲存變更」（刪除、復原單道餐點、改備註）和「取消整筆訂單」成功之後
	//   方法：用訂單編號查出訂單 → 拿到桌位 → 算出那一組客人的代號（和點餐時 WebSocket 分組用的一樣，例如 A01:183042）
	//         → 傳 order-changed 給那一組的手機，手機會在背景更新「已點餐點」並提示「餐廳已更新您的已點餐點」
	//   不通知的情況：查不到訂單、客人已經離席（桌位的入座時間是 null，算不出代號）
	//   通知失敗（客人手機剛好斷線）只會略過，不影響店員的操作；客人下次打開頁面一樣會看到最新的狀態
	public void notifyTableOrderChanged(Integer orderId) {
		if (orderId == null) return;
		OrdersVO order = ordersRepository.findById(orderId).orElse(null);
		if (order == null) return;
		SeatVO seat = order.getSeatNo();
		if (seat == null || seat.getSeatTime() == null) return;
		cartWebSocketHandler.notifyOrderChanged(frontSeatService.getSeatingKey(seat));
	}

	// 卡片清單
	private Map<Integer, MenuMealCardDTO> toCardMap(List<MenuMealCardDTO> cards) {
	    return cards.stream()
	                .collect(Collectors.toMap(MenuMealCardDTO::getMealNo, Function.identity()));
	}
	
	public List<CartItemDTO> getCartItems(String seatingKey, List<MenuMealCardDTO> cards) {
	    Map<Integer, MenuMealCardDTO> cardMap = toCardMap(cards);
	    List<CartItemDTO> items = new ArrayList<>();

	    for (CartLineDTO line : cartRedisSve.getLines(seatingKey)) {
	        MenuMealCardDTO card = cardMap.get(line.getMealNo());

	        // 找不到卡片 → 這道菜已經下架了：從購物車拿掉，不顯示
	        if (card == null) {
	            cartRedisSve.remove(seatingKey, line.getMealNo(), line.getComment());
	            continue;
	        }
	        items.add(new CartItemDTO(card.getMealNo(), card.getMealName(),
	                                  card.getPrice(), line.getNum(), line.getComment()));
	    }
	    return items;
	}

	@Transactional
	public String submitOrder(SeatVO seat, MemberVO member) {

	    //讀購物車：空的就不用送
	    String seatingKey = frontSeatService.getSeatingKey(seat);
	    List<CartLineDTO> lines = cartRedisSve.getLines(seatingKey);
	    if (lines.isEmpty()) {
	        return "購物車是空的。";
	    }

	    //查「這一刻」的活動價和菜單卡片
	    Map<Integer, PromoPriceDTO> activePrices = promoteSve.getActivePrices();
	    Map<Integer, MenuMealCardDTO> cardMap = toCardMap(menuSve.getMenuCards(activePrices));

	    //先檢查：購物車裡有沒有已經下架的菜（有的話整張都不送，什麼都不寫進資料庫）
	    boolean hasSoldOut = lines.stream()
	                              .anyMatch(line -> !cardMap.containsKey(line.getMealNo()));
	    if (hasSoldOut) {
	        return "購物車裡有已經售完的餐點，請重新確認購物車。";
	    }

	    //找這一組還沒結帳的訂單；沒有就新增一張
	    Integer orderNo = frontSeatService.getUnpaidOrderNo(seat);

	    int cartTotal = lines.stream()
                        .mapToInt(line -> cardMap.get(line.getMealNo()).getPrice() * line.getNum())
                        .sum();
	    int orderedTotal = (orderNo == null) ? 0
	    					: ordersRepository.findById(orderNo).orElseThrow().getOrdersActualPrice();   // 已取消的不算
	    if (cartTotal == 0 && orderedTotal == 0) {
	    	return "購物車裡只有 0 元的餐點，請至少加點一樣有價格的餐點再送出訂單。";
	    }

	    OrdersVO order;
	    if (orderNo == null) {
	        order = new OrdersVO();
	        order.setSeatNo(seat);
	        order.setMember(member); // 沒有登入就是 null
	        order.setOrdersTime(LocalDateTime.now());
	        order.setOrdersDetails(new ArrayList<>());      //空的清單，下面要放明細進去
	        order = ordersRepository.save(order);           // 存了之後才有訂單編號
	        // 整單折扣：無條件的活動自動套用（只在新建訂單這一刻；加點不加，店員取消過的才不會被加回去）
	        // 不用另外重算：這個方法最後的 ordersSve.recalculate 會把整單折扣算進去
	        OrderPromoteOptionDTO auto = promoteSve.getAutoApplyPromote();   // 進行中、沒有條件、折數最多的那一個；沒有是 null
	        if (auto != null) {
	            order.setPromote(promoteSve.getPromoteRef(auto.getPromoteNo()));   // 只拿參照，不查圖片
	            order.setOrdersPromoteDisc(auto.getPromoteDisc());                // 存折數快照
	        }
	    } else {
	        order = ordersRepository.findById(orderNo).orElseThrow();
	        if (order.getMember() == null && member != null) {
	            order.setMember(member);                    // 加點時才登入：補上會員
	        }
	    }

	    //購物車的每一列 → 一筆訂單明細
	    for (CartLineDTO line : lines) {
	        MenuMealCardDTO card = cardMap.get(line.getMealNo());
	        PromoPriceDTO promo = activePrices.get(line.getMealNo());
	        int num = line.getNum();

	        OrdersDetailsVO detail = new OrdersDetailsVO();
	        detail.setOrders(order);
	        detail.setMeal(menuRepository.getReferenceById(line.getMealNo()));
	        detail.setPromote(promo != null ? promo.getPromote() : null);
	        detail.setOdMealNum(num);
	        detail.setOdDiscountPrice(card.getOrigPrice());
	        detail.setOdDiscountTotal((card.getOrigPrice() - card.getPrice()) * num);
	        detail.setOdActualPrice(card.getPrice() * num);
	        detail.setOdComment(line.getComment().isEmpty() ? null : line.getComment());
	        detail.setOdStatus("製作中");

	        ordersDetailsRepository.save(detail);
	        order.getOrdersDetails().add(detail);
	    }

	    //重算訂單的三個金額，最後才清空購物車
	    ordersSve.recalculate(order.getOrders());
	    cartRedisSve.clear(seatingKey);
	    return null;
	}
	
	public List<OrderedItemDTO> getOrderedItems(SeatVO seat){
		Integer no = frontSeatService.getUnpaidOrderNo(seat);
		return (no != null) ? orderedItemRepository.findOrderedItems(no) : List.of();
	}
	
    public byte[] getMealImg(Integer mealNo) {
    	return menuRepository.findImgByMealNo(mealNo);
    }
}
