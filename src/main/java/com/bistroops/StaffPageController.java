package com.bistroops;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// 員工後台的頁面（平板使用）
// 目前只負責「打開頁面」，畫面上的資料是 static/js/staff/staff.js 裡的假資料。
//
// 【Thymeleaf 串接】接上資料庫時，這個 Controller 要做的事（總覽）
//
//   做法和會員功能相同，不用 JSON：
//     顯示資料：在方法加上 Model 參數，把資料放進去，HTML 用 th:each／th:text 顯示
//     修改資料：畫面上的按鈕包成表單送到 @PostMapping 的方法，
//               改完資料庫後 return "redirect:/staff/..." 回到原本的頁面
//
//   ★ HTML 這一邊已經寫好了：每個後台頁面裡都有【資料庫資料】（Thymeleaf）和【假資料】（JS）兩份。
//     【資料庫資料】在 Model 沒有資料時不會出現；這裡把資料放進 Model 之後，它就會和假資料同時出現在頁面上，
//     可以對照著檢查。確認沒問題後，再照各頁 HTML 開頭寫的步驟刪掉那一頁的假資料。
//     所以可以一頁一頁做：先做結帳，結帳的資料庫版本出現、確認、刪掉假資料，再做下一頁。
//
//     各頁「出現的條件」：
//       主頁        Model 有 nextRoundCount
//       工作面板    Model 有 orders（對話框另外要有 editOrder）
//       訂單管理    Model 有 orders
//       結帳        Model 有 orders（右邊的明細另外要有 order）
//     查不到資料時請放「空的清單」而不是 null（空的清單會顯示「目前沒有…」，null 會讓整塊不出現）。
//
//   每個網址需要的資料與動作（Model 的名稱可以自己改，但要和 HTML 裡寫的一致）：
//
//   網址                               方式   要做的事
//   /staff                             GET    Model 放 nextRoundTime、nextRoundCount、waitingCount、callingNumber（訂候位長條）
//   /staff/workboard?view=kitchen      GET    Model 放 orders（還有餐點沒送達的未結帳訂單，依下單時間排序）和 view
//   /staff/workboard?view=floor        GET    同上
//   /staff/workboard?view=done         GET    Model 放 orders（餐點全部送達、尚未結帳的訂單）和 view
//   /staff/workboard/toServe           POST   收 itemId：餐點狀態 製作中 → 等待送餐（內場按「出餐」）
//   /staff/workboard/backToCooking     POST   收 itemId：餐點狀態 等待送餐 → 製作中（內場誤觸出餐時退回）；已經是 已送達 的不要動
//   /staff/workboard/served            POST   收 itemId：餐點狀態 等待送餐 → 已送達，並記下送達時間（外場按「確認送達」）
//   /staff/workboard/edit?orderId=     GET    和 workboard 相同（view 是 kitchen），另外 Model 多放 editOrder（要編輯的那筆訂單）
//                                              和 canCancel（餐點都還沒出餐，可以刪除整筆訂單時是 true），頁面會打開對話框
//   /staff/workboard/notes             POST   收 orderId、itemIds、itemNotes、cancelledItemIds（最後一個要加 required = false）：
//                                              itemIds 和 itemNotes 是順序相同的兩個 List，第幾個備註就是第幾個餐點的；
//                                              在 cancelledItemIds 裡的餐點標記成已取消，不在裡面但原本已取消的改回製作中
//   /staff/workboard/cancel            POST   收 orderId：把這筆訂單所有訂單明細的狀態都改成「已取消」（訂單 orders 本身不動）
//   /staff/orders?status=&keyword=&date=   GET   Model 放 orders（依狀態與關鍵字查出來的訂單）、status、keyword。
//                                              status 是空的（全部）、PAID（已結帳）或 CANCELLED（已取消）時只查 date 那一天的：
//                                              已結帳依結帳日期，其他（包含已取消）依下單日期；
//                                              沒帶 date 或日期在今天之後就用今天。
//                                              另外放 date、dateText、prevDate、nextDate、today 給日期切換使用（見 orders.html）
//   /staff/checkout                    GET    Model 放 orders（未結帳、未取消的訂單）
//   /staff/checkout?order=             GET    同上，另外 Model 多放 order（要結帳的那筆），右邊會顯示餐點明細
//   /staff/checkout/pay                POST   收 orderId、method（刷卡／現金）：把訂單標記成已結帳，記下付款方式與時間
//
//   ★★ 欄位名稱目前都是「範例」★★
//     下面這些名稱（以及上面表格裡 Model 的名稱 orders、order、editOrder 等）只是照假資料的欄位先取的，不是規定。
//     VO 要怎麼設計（要幾個類別、屬性叫什麼、要不要照資料表的欄位取名），請負責的組員照自己的想法決定。
//     決定之後，把各頁 HTML【資料庫資料】裡 ${...} 用到的名稱改成 VO 實際的名稱；兩邊一樣就可以，
//     對不上的話頁面會出現錯誤（Thymeleaf 找不到那個屬性）。
//
//   HTML 目前用到的名稱（改名時可以對照這張清單，看哪些地方要一起改）：
//     訂單  id、table 桌號、time 下單時間、items 餐點清單、
//           total 總價、discountTotal 總折扣、payAmount 實付金額、allServed 餐點是否全部送達、
//           servedCount 已送達幾道、activeCount 沒被取消的共幾道（工作面板）、
//           statusText 狀態的中文、statusClass 標籤顏色的 class、payInfo 結帳或取消的說明（訂單管理）
//     餐點  id、mealtype 分類、name、qty、price、discount、status、note、servedAt 送達時間
//   用算的欄位（allServed、payAmount 等）可以在 VO 加上 getter，或另外寫一個給畫面用的類別。
//
//   各頁面 HTML 與 JS 裡對應的位置都標了「Thymeleaf 串接」，用 Ctrl+F 搜尋就能找到。
//   假資料的欄位與狀態（訂單、餐點各有哪些欄位、狀態有哪幾種）寫在 static/js/staff/staff.js 的 B-1 開頭。
@Controller
@RequestMapping("/staff")
public class StaffPageController {

	// 主頁：左邊選單 + 歡迎畫面
	// 【Thymeleaf 串接】加上 Model，放入訂候位長條要顯示的四個數字（見 staff_index.html 的「訂候位管理」）
	// @GetMapping("")
	// public String index() {
	// 	return "staff/staff_index";
	// }

	// 工作面板：內場／外場／已完成訂單(未結帳)
	// 【Thymeleaf 串接】加上 @RequestParam(defaultValue = "kitchen") String view 和 Model，
	//   依照 view 查出要顯示的訂單放進 Model（見 workboard.html 的卡片區）
	@GetMapping("/workboard")
	public String workboard() {
		return "staff/workboard";
	}

	// 訂單管理：查詢所有訂單
	// 【Thymeleaf 串接】加上 @RequestParam(required = false) String status、String keyword、String date 和 Model，
	//   查出符合條件的訂單放進 Model（見 orders.html 的篩選、日期切換與清單）
	@GetMapping("/orders")
	public String orders() {
		return "staff/orders";
	}

	// 結帳
	// 【Thymeleaf 串接】加上 @RequestParam(required = false) String order 和 Model，
	//   放入未結帳的訂單清單；有帶 order 時，把那一筆也放進去（見 checkout.html）
	@GetMapping("/checkout")
	public String checkout() {
		return "staff/checkout";
	}

}
