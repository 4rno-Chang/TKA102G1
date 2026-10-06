package com.bistroops;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// 前台的「預約訂位」與「現場候位」頁面
//
// 目前只負責「打開頁面」。記得寫java時領回自己的資料夾時作並刪除這邊。
// 畫面上的資料（可預約的日期、時段、候位號碼）
// 都是 JS 裡的假資料（訂位在 static/js/front/reservation.js 第 1 區、候位在 static/js/front/waiting.js 第 1 區），
// 頁面之間用瀏覽器的暫存傳遞。
//
// 【Thymeleaf 串接】接上資料庫時，這個 Controller 要做的事（總覽）
//
//   做法和會員功能相同，不用 JSON：
//     顯示資料：在方法加上 Model 參數，把資料放進去，HTML 用 th:each／th:text 顯示
//     送出資料：表單送到 @PostMapping 的方法，存進資料庫後 return "redirect:..." 到結果頁
//
//   網址                              方式   要做的事
//   /bistroops/reservation            GET    收 date、tableType（選了日期或換桌型時，頁面會帶著這兩個值重新載入）
//                                            Model 放：openDates 可預約的日期（逗號接起來的文字）、minDate、maxDate、
//                                                      tableType、date、dateText、
//                                                      slots 那天的時段（每一個有 rsvDtNo 時段編號、timeText 時間、full 是否額滿）
//   /bistroops/reservation/contact    POST   收 rsvDtNo、tableType；確認還能預約後放進 Model，顯示填資料的頁面
//                                            （現在是 GET，接資料庫時改成 @PostMapping）
//   /bistroops/reservation/submit     POST   （要新增）收 rsvDtNo、tableType、name、phone、email、note、agree
//                                            檢查資料 → 再確認一次那個時段還有空位
//                                            → 用 phone 查會員，沒有就先建立會員，拿到 mem_no（見下面的 ★ 訂位與會員）→ 存進資料庫
//                                            成功：用 RedirectAttributes 帶訂位資料，redirect 到 /bistroops/reservation/done
//                                            失敗：把錯誤訊息 error 和客人填的內容放回 Model，回到填資料的頁面
//   /bistroops/reservation/done       GET    顯示訂位結果（資料來自上一步的 RedirectAttributes，或用訂位編號從資料庫查）
//   /bistroops/waiting                GET    Model 放：open 是否開放取號、refreshTime 現在時間、
//                                                      queues 三個桌型（tableType、callingNo 目前叫號、waitingCount 等待組數、estimate 預估等候）
//   /bistroops/waiting/join           GET    取號表單（給客人掃 QR code 開啟，網站上沒有連結）。Model 放 open、queues
//   /bistroops/waiting/join           POST   （要新增）收 tableType、name、phone、note（備註，選填）；檢查 → 存進資料庫並產生號碼 → redirect 到號碼牌
//   /bistroops/waiting/ticket         GET    顯示號碼牌（建議用網址上的手機或號碼從資料庫查，客人重新整理才不會不見）
//
//   查詢與取消（不需要登入會員，只用手機查）：
//   /bistroops/reservation?lookupPhone=   GET    （加在訂位步驟一的方法裡）有帶 lookupPhone 時，查出那支手機今天以後的所有訂位，
//                                                Model 多放 lookupPhone 和 lookupList，頁面上的「查詢訂位」對話框會直接打開
//   /bistroops/reservation/cancel         POST   （要新增）收 reservationNo、phone；確認這筆訂位的手機和 phone 相同後標記成已取消，
//                                                redirect 回 /bistroops/reservation?lookupPhone=同一支手機
//   /bistroops/waiting?lookupPhone=       GET    （加在候位狀況的方法裡）有帶 lookupPhone 時，查出那支手機今天等候中的號碼牌，
//                                                Model 多放 lookupPhone 和 lookupTickets（每一張有 ticketNo、tableType、ahead、estimate、takenTime、note）
//   /bistroops/waiting/cancel             POST   （要新增）收 ticketNo、phone；確認手機相同後把那張號碼牌標記成已取消，
//                                                redirect 回 /bistroops/waiting?lookupPhone=同一支手機
//   訂位和候位都只能取消、不能修改。取消時不要從資料庫刪掉，改成標記狀態，之後才查得到紀錄。
//
//   桌型在網址和表單裡用數字 2／4／6 表示（幾人桌），畫面上才顯示成「2 人桌」。這個數字就是資料表 seat_type 的 seat_type_no。
//
//   資料從哪裡來（資料表依照 資料庫表格建立/createTable_v1_4.sql，詳細說明在 reservation.js、waiting.js 第 1 區開頭）：
//     可預約的日期、每天的時段   訂位時段 reservation_datetime（一列是某一天的某個時段，rsv_dt_no 是時段編號）
//     某個時段是否額滿           訂位 reservation 的筆數 和 桌型 seat_type 的 seat_type_rsv_num（開放訂位桌數）比較
//     送出訂位                   新增一筆 reservation（mem_no、rsv_dt_no、seat_type_no、rsv_status、rsv_comment…）
//     查詢訂位                   用手機查 會員 member（mem_tel）拿到 mem_no，再查那個 mem_no 的 reservation
//     目前叫號、等待組數         桌型 seat_type 的 seat_type_call_num（入座號）、seat_type_take_num（取號）
//     取號                       seat_type_take_num 加 1，並新增一筆 候位 waiting
//
//   ★ 提醒：候位只算當日，但 候位 waiting 沒有日期欄位，所以後臺要另外寫一個「每日刷新候位資料」的排程
//     （每天固定時間清空 waiting、把 seat_type 的取號與入座號歸零）。做法與注意事項寫在 waiting.js 第 1 區開頭。
//
//   ★ 提醒：目前的資料庫沒有地方存「號碼牌的號碼」（候位 waiting 的資料列沒有記客人拿到幾號）。
//     組內已決定另外建立資料庫的表格來存，請負責的組員記得建立，並把語法加進 資料庫表格建立 的 SQL 檔。
//     說明寫在 waiting.js 第 1 區開頭。
//
//   ★ 訂位與會員：訂位 reservation 只存會員編號 mem_no（必填），沒有姓名、電話、Email 的欄位。
//     組內決定的做法：送出訂位時用客人填的電話查會員（mem_tel），查得到就用那位會員的 mem_no；
//     查不到就先新增一位會員（mem_tel、mem_name、mem_mail），再用新的 mem_no。已登入的客人直接用 session 裡的 mem_no。
//     這樣建立的會員沒有密碼，會員的註冊與登入要一起調整。要留意的地方寫在 reservation.js 第 1 區開頭。
//
//   有登入會員時，可以從 session 取出會員（東西是俊佑做的可以去他那邊看會員的Controller）做出可以同步會員資料填寫的部分，
//
//   每個頁面的 HTML 裡都寫了「接上資料庫後這一塊要換成什麼」，用 Ctrl+F 搜尋「Thymeleaf 串接」可以找到。
//   假資料的規則（哪些日期可以訂、號碼怎麼編可以再討論）寫在 static/js/front/reservation.js 與 waiting.js 的第 1 區，可以當作寫 Service 時的參考。
@Controller
@RequestMapping("/bistroops")
public class BookingPageController {

	// 訂位步驟一：選擇日期、桌型、時段
	@GetMapping("/reservation")
	public String reservationDate() {
		return "front/reservation/reservation_date";
	}

	// 訂位步驟二：填寫聯絡資料
	@GetMapping("/reservation/contact")
	public String reservationContact() {
		return "front/reservation/reservation_contact";
	}

	// 訂位步驟三：訂位成功
	@GetMapping("/reservation/done")
	public String reservationDone() {
		return "front/reservation/reservation_done";
	}

	// 候位狀況
	@GetMapping("/waiting")
	public String waitingStatus() {
		return "front/waiting/waiting_status";
	}

	

}
