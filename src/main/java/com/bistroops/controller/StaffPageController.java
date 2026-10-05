// package com.bistroops.controller;

// import org.springframework.stereotype.Controller;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;

// // 員工後台的頁面（平板使用）
// // 目前只負責「打開頁面」，畫面上的資料是 static/js/staff.js 裡的假資料。
// //
// // 【Thymeleaf 串接】接上資料庫時，這個 Controller 要做的事（總覽）
// //
// //   做法和會員功能相同，不用 JSON：
// //     顯示資料：在方法加上 Model 參數，把資料放進去，HTML 用 th:each／th:text 顯示
// //     修改資料：畫面上的按鈕包成表單送到 @PostMapping 的方法，
// //               改完資料庫後 return "redirect:/staff/..." 回到原本的頁面
// //
// //   每個網址需要的資料與動作（Model 的名稱可以自己改，但要和 HTML 裡寫的一致）：
// //
// //   網址                               方式   要做的事
// //   /staff                             GET    Model 放 nextRoundTime、nextRoundCount、waitingCount、callingNumber（訂候位長條）
// //   /staff/workboard?view=kitchen      GET    Model 放 orders（還有餐點沒送達的未結帳訂單，依下單時間排序）和 view
// //   /staff/workboard?view=floor        GET    同上
// //   /staff/workboard?view=done         GET    Model 放 orders（餐點全部送達、尚未結帳的訂單）和 view
// //   /staff/workboard/toServe           POST   收 itemId：餐點狀態 COOKING → TO_SERVE（內場按「出餐」）
// //   /staff/workboard/served            POST   收 itemId：餐點狀態 TO_SERVE → SERVED，並記下送達時間（外場按「確認送達」）
// //   /staff/workboard/edit?orderId=     GET    和 workboard 相同，另外 Model 多放 editOrder（要編輯的那筆訂單），頁面會打開對話框
// //   /staff/workboard/notes             POST   收 orderId、orderNote、itemNotes、cancelledItemIds：儲存備註、把指定的餐點標記成已取消
// //   /staff/workboard/cancel            POST   收 orderId、reason：把訂單標記成已取消，記下原因與時間
// //   /staff/orders?status=&keyword=     GET    Model 放 orders（依狀態與關鍵字查出來的訂單）、status、keyword
// //   /staff/checkout                    GET    Model 放 orders（未結帳、未取消的訂單）
// //   /staff/checkout?order=             GET    同上，另外 Model 多放 order（要結帳的那筆），右邊會顯示餐點明細
// //   /staff/checkout/pay                POST   收 orderId、method（刷卡／現金）：把訂單標記成已結帳，記下付款方式與時間
// //
// //   各頁面 HTML 與 JS 裡對應的位置都標了「Thymeleaf 串接」，用 Ctrl+F 搜尋就能找到。
// //   資料的欄位（訂單、餐點各有哪些欄位、狀態有哪幾種）寫在 static/js/staff.js 第 1 區的開頭。
// @Controller
// @RequestMapping("/staff")
// public class StaffPageController {

// 	// 主頁：左邊選單 + 歡迎畫面
// 	// 【Thymeleaf 串接】加上 Model，放入訂候位長條要顯示的四個數字（見 staff_index.html 的「訂候位管理」）
// 	// @GetMapping("")
// 	// public String index() {
// 	// 	return "staff/staff_index";
// 	// }

// 	// 工作面板：內場／外場／已完成訂單(未結帳)
// 	// 【Thymeleaf 串接】加上 @RequestParam(defaultValue = "kitchen") String view 和 Model，
// 	//   依照 view 查出要顯示的訂單放進 Model（見 workboard.html 的卡片區）
// 	@GetMapping("/workboard")
// 	public String workboard() {
// 		return "staff/workboard";
// 	}

// 	// 訂單管理：查詢所有訂單
// 	// 【Thymeleaf 串接】加上 @RequestParam(required = false) String status、String keyword 和 Model，
// 	//   查出符合條件的訂單放進 Model（見 orders.html 的篩選與清單）
// 	@GetMapping("/orders")
// 	public String orders() {
// 		return "staff/orders";
// 	}

// 	// 結帳
// 	// 【Thymeleaf 串接】加上 @RequestParam(required = false) String order 和 Model，
// 	//   放入未結帳的訂單清單；有帶 order 時，把那一筆也放進去（見 checkout.html）
// 	@GetMapping("/checkout")
// 	public String checkout() {
// 		return "staff/checkout";
// 	}

// }
