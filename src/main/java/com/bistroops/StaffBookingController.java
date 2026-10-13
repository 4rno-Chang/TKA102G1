package com.bistroops;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/staff")
public class StaffBookingController {

	// ══════════════════════════════════════════════════════════════════════════════
	// 後台「訂候位」：工作人員管理訂位、候位的頁面
	//   頁面：templates/staff/booking.html（一個頁面、兩個分頁：訂位／候位）
	//   CSS ：static/css/staff/booking.css　　JS：static/js/staff/booking.js
	//   網址：/staff/booking（訂位）、/staff/booking?tab=waiting（候位）
	// ══════════════════════════════════════════════════════════════════════════════
	//
	// 目前只有最下面一個方法，負責「打開頁面」和告訴頁面現在是哪一個分頁；
	// 畫面上的資料是 booking.js 裡的假資料。
	//
	// 【Thymeleaf 串接】booking.html 裡【資料庫資料】的那一份已經用 Thymeleaf 寫好了，
	//   這裡把資料放進 Model 之後，它就會和假資料同時出現在頁面上，可以對照著看。
	//
	// ★★ 欄位名稱、Model 的名稱都是「範例」★★
	//   下面寫的 Model 名稱（rsvList、meal、waitingList、seatBoards…）、表單欄位的名稱是我先取的，
	//   組員可以照自己的想法改，只要這裡放進去的名稱和 booking.html 裡用的相同就好。
	//
	//
	// ══ 這一頁的規定（先看這裡；每一條後面寫了要在哪裡設定）══
	//
	//   (A) 餐期：午餐／晚餐　★ 尚可再討論
	//       營業時間先當作 11:00～14:00（午餐）和 17:00～21:00（晚餐）。訂位的列表一次只看「某一天的午餐」或「某一天的晚餐」。
	//       分界先定在 15:30（14:00 和 17:00 的中間）：
	//         ・訂位時段在 15:30 以前 → 午餐；15:30（含）以後 → 晚餐
	//         ・剛打開頁面（網址沒有帶 meal）：現在的時間在 15:30 以前 → 午餐；之後 → 晚餐
	//           （也就是「現在比較接近中午還是晚上」）
	//       網址和 Model 裡用文字表示餐期："lunch"（午餐）、"dinner"（晚餐）。
	//       要設定的地方：
	//         ・這個檔案：建議寫成常數，之後要改只改一個地方，例如
	//             private static final LocalTime MEAL_SPLIT = LocalTime.of(15, 30);
	//         ・booking.js 假資料的 MEAL_SPLIT（刪掉假資料之後就沒有了）
	//       （booking.html 的午餐、晚餐按鈕上不顯示時間，所以頁面不用改。）
	//       討論之後如果營業時間或分界改了，改上面兩個地方。
	//
	//   (B) 可以訂的時段：先固定四個　11:00、12:30（午餐）、17:00、17:30（晚餐）
	//       目前是「寫死」在 booking.html 新增／修改訂位的對話框裡，每一天都一樣；表單送回來的是 rsvTime（例如 "17:00"）這種文字。
	//       ★ 訂位表（reservation）存的不是時間，是「訂位時段編號」（rsv_dt_no），所以儲存的時候要自己把日期和時間合起來，
	//         去訂位時段表（reservation_datetime）找那一筆，做法寫在步驟 3。
	//       要設定的地方：
	//         ・booking.html 對話框「訂位時段」那一段的兩個清單（午餐一個、晚餐一個）
	//         ・這個檔案儲存時檢查「送來的時間是不是這四個之一」的地方（建議也寫成常數，例如 List.of("11:00", "12:30", "17:00", "17:30")）
	//         ・booking.js 假資料的 RSV_TIMES
	//         ・★ 資料庫的訂位時段表要有這些時間的資料，不然儲存時會找不到（見步驟 3）
	//       之後想改成「從資料庫拿那一天可以訂的時段」的做法寫在步驟 2 的最後。
	//
	//   (C) 狀態是「取消」的不顯示
	//       訂位、候位兩個列表都不列出狀態是「取消」的資料（資料還在資料庫裡，只是這一頁不顯示）。
	//       要設定的地方：步驟 1、步驟 5 查詢的時候就把「取消」排除（在 Repository 的查詢條件加上狀態不等於取消，
	//       或查出來之後用迴圈把取消的拿掉）。booking.html 也會再濾一次當保險，但請不要只靠頁面：
	//       只靠頁面的話，「共幾筆」以外用到這份清單的地方（例如之後做分頁）會算錯。
	//
	//   (D) ★★ 入座不在這一頁處理 ★★
	//       訂位的「實到」、候位的「已入座」，請由負責【入座管理】的組員，在管理入座（安排客人入座）的時候修改這些資料的狀態：
	//         ・訂位的客人到了、安排入座 → 把那筆訂位的 rsvStatus 改成「實到」
	//         ・候位的客人回來、安排入座 → 把那筆候位的 waitingStatus 改成「已入座」（WaitingService.seatWaiting 已經有這個方法）
	//         ・入座會用掉桌子：目前可用的桌數（seat_type.seat_type_pending，之後是 Redis）也由入座管理更新
	//       所以這一頁：沒有「入座」的按鈕；修改訂位的狀態選不到「實到」；修改候位的狀態選不到「已入座」。
	//       這一頁只負責顯示它們（入座管理改了之後，按「刷新頁面」就看得到）。
	//       這個 Controller 儲存的時候也要守住這條規定，寫在步驟 3 和步驟 7。
	//
	//   (E) 刷新頁面
	//       頁面右上角的「刷新頁面」是一個連結，連回目前這個畫面的網址（訂位帶著 date、meal、keyword；候位是 ?tab=waiting）。
	//       等於再做一次步驟 1 或步驟 5 的查詢，所以「不用另外寫方法」。
	//       別台裝置新增、取消的訂位候位，或入座管理改的狀態，按了之後就會出現。
	//       ★ 這一頁不會自己更新，要按了才會重新查。想要每隔一段時間自動更新，是之後另外的功能。
	//
	//
	// ★ 可以直接用的東西（你已經寫好的）：
	//     ReservationService          getAll()、search(date, status)（查某一天的訂位）、addForMember(...)
	//     ReservationDatetimeService  getAvailableByDate(date)（某一天可以訂的時段）
	//     WaitingService              getAll()、getOneWaiting(waitingNo)、addWaiting(...)、
	//                                 notifyWaiting、seatWaiting、missWaiting、cancelByStaff（改狀態，裡面已經會檢查狀態對不對）
	//                                 狀態的文字：候位中、已通知、已入座、取消、過號（WaitingService 的常數）
	//     SeatTypeRepository          findAll()（所有桌型）
	//   用之前先看一下那個方法收什麼參數、回傳什麼；不夠用的再加。
	//
	// ── 總覽：最後會有這些方法 ──
	//   步驟  網址                                    回傳                          要放進 Model
	//    1    GET  /staff/booking                     "staff/booking"               tab、rsvList、date、meal、keyword（message 可以不放）
	//    2    GET  /staff/booking/rsv/add             "staff/booking"               步驟 1 的全部，再加 rsvForm、formDate、formTime、seatTypes
	//    2    GET  /staff/booking/rsv/edit            "staff/booking"               同上（rsvForm 是查出來的那一筆）
	//    3    POST /staff/booking/rsv/save            成功 redirect；不通過回傳頁面  不通過時：步驟 2 的全部，再加 errorMsg
	//    4    POST /staff/booking/rsv/cancel          redirect                      用 RedirectAttributes 帶 message
	//    5    GET  /staff/booking?tab=waiting         "staff/booking"               tab、waitingList、seatBoards
	//    6    GET  /staff/booking/waiting/add         "staff/booking"               步驟 5 的全部，再加 waitingForm、seatTypes
	//    6    GET  /staff/booking/waiting/edit        "staff/booking"               同上（waitingForm 是查出來的那一筆）
	//    7    POST /staff/booking/waiting/save        成功 redirect；不通過回傳頁面  不通過時：步驟 6 的全部，再加 waitingError
	//    8    POST /staff/booking/waiting/miss        redirect                      用 RedirectAttributes 帶 message
	//    9    POST /staff/booking/waiting/call        redirect                      同上
	//   建議的順序：1 → 5（先讓兩個列表出現）→ 2、3、4（訂位的新增、修改、取消）→ 6、7、8、9（候位的操作）
	//   每做完一個就打開頁面看【資料庫資料】有沒有出現，不要全部寫完才測。
	//
	//
	// ══ 寫之前先看：容易出錯的地方 ══
	//   (1) 回傳的頁面名稱是 "staff/booking"（檔案是 templates/staff/booking.html，直接放在 staff 資料夾裡，沒有另外的子資料夾）。
	//       不要和網址 /staff/booking 搞混：長得一樣，但 return 的是「頁面檔案的名稱」，不是網址。
	//   (2) 每個「回傳頁面」的方法都要放 tab，不然分頁的按鈕和要顯示哪一塊會不對。
	//       顯示對話框（步驟 2、6，以及步驟 3、7 不通過）的時候，後面的列表也要照常放，不然對話框後面會是空的。
	//       建議把「查列表、放進 Model」寫成自己用的 private 方法（訂位一個、候位一個），各個方法都呼叫它。
	//   (3) 查不到資料要放「空的清單」，不要放 null（null 會讓資料庫那一份整塊不出現）。
	//   (4) 參數都加 required = false，再自己用 if 檢查；沒有加的話少了參數會直接變成 400 錯誤頁。
	//   (5) 日期：畫面上是用自製的月曆選的，送來的是 2026-10-14 這種文字（只有年月日，沒有時間），用 LocalDate 接，
	//       並加上 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)（組員的 ReservationController 就是這樣寫的）。
	//       訂位的「時段」不在月曆裡，是對話框裡另外一排可以點的時段（見步驟 2、3）。
	//   (6) ★ LazyInitializationException：ReservationVO、WaitingVO 裡的 member、rsvDt、seatType 都是 fetch = LAZY，
	//       頁面上會用到 r.member.memName、r.rsvDt.rsvDtDatetime、w.seatType.seatTypeNo。
	//       如果打開頁面出現這個錯誤，代表查詢的時候沒有把這些一起查出來：
	//       在 Repository 的查詢用 JOIN FETCH 一次查好，或確認 application.properties 沒有把 spring.jpa.open-in-view 關掉。
	//   (7) 會改資料的動作（儲存、取消訂位、過號、叫號…）一律用 @PostMapping，做完用 redirect 回列表，
	//       不要直接回傳頁面：直接回傳的話，使用者按重新整理會把同一個動作再送一次。
	//   (8) 頁面上的檢查（必填、格式）、按鈕的顯示／隱藏、下拉選單有哪些選項都可以被跳過，Java 一定要自己再檢查一次。
	//   (9) 編號的型別：ReservationVO 的 rsvNo、WaitingVO 的 waitingNo 是 Long；SeatTypeVO 的 seatTypeNo、MemberVO 的 memNo 是 Integer。
	//       參數的型別要跟著寫對。
	//
	//
	// ══ 步驟 1：訂位的查詢（改下面的 booking 方法）══
	//   參數：@RequestParam(required = false) String tab
	//         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	//         @RequestParam(required = false) String meal
	//         @RequestParam(required = false) String keyword
	//         Model model
	//   tab 是 "waiting" → 做步驟 5 的事；其他 → 做下面的事。
	//   先把條件整理好：
	//     date：沒有帶 → LocalDate.now()
	//     meal：是 "lunch" 或 "dinner" → 照用；沒有帶、或帶了別的字 → 依現在的時間決定（規定 A）：
	//             LocalTime.now().isBefore(MEAL_SPLIT) ? "lunch" : "dinner"
	//   查詢的兩種情況：
	//     keyword 有內容（去掉空白之後不是空的）→ 找「所有日期」裡姓名有這些字、或電話有這些數字的訂位，包含過往的；不看 date 和 meal
	//     沒有 keyword                          → 那一天、那個餐期的訂位
	//         午餐：訂位時段 >= 那一天 00:00，而且 < 那一天 15:30　　date.atStartOfDay() ～ date.atTime(MEAL_SPLIT)
	//         晚餐：訂位時段 >= 那一天 15:30，而且 < 隔天 00:00　　　date.atTime(MEAL_SPLIT) ～ date.plusDays(1).atStartOfDay()
	//   兩種情況都要排除狀態是「取消」的（規定 C），結果依訂位時段排序（早的在前面）。
	//   放進 Model：
	//     tab      "rsv"
	//     rsvList  查出來的訂位（List<ReservationVO>）
	//     date     顯示的日期（LocalDate。有 keyword 的時候也要放，日期欄位才有值）
	//     meal     顯示的餐期（"lunch" 或 "dinner"。有 keyword 的時候也要放）
	//     keyword  搜尋的文字
	//   要注意：
	//     ・ReservationService.search(date, null) 可以查「某一天」，但沒有分餐期、也沒有排除取消。
	//       可以先用它查出來，再用迴圈挑出餐期對、狀態不是取消的；或請負責訂位的組員在 ReservationRepository 加查詢
	//       （用「訂位時段在兩個時間之間、而且狀態不等於取消」當條件）。
	//     ・用姓名、電話查的方法目前沒有，要在 ReservationRepository 加查詢
	//       （訂位表裡只有會員編號，姓名和電話在會員表，所以條件要寫成 r.member.memName、r.member.memTel）。
	//     ・「某一天」是指訂位時段（rsvDt.rsvDtDatetime）落在那一天，不是訂位成立的時間（rsvCreateTime）。
	//
	//
	// ══ 步驟 2：新增／修改訂位的對話框（新增兩個方法）══
	//   對話框靠 Model 裡有 rsvForm 才會出現。兩個方法都要先做步驟 1 的事（後面的列表才會在），再多放：
	//     rsvForm    要填的訂位（ReservationVO）
	//     formDate   對話框裡選的日期（LocalDate）
	//     formTime   對話框裡選的時段（String，例如 "17:00"；null 代表還沒選）
	//     seatTypes  所有桌型（List<SeatTypeVO>）
	//
	//   新增：@GetMapping("/booking/rsv/add")
	//     參數：date、meal（目前看的那一天和餐期，後面的列表用；都是 required = false）
	//     rsvForm 放 new ReservationVO()；formDate 用 date（沒有帶、或是今天以前的日子就用今天）；formTime 不用放。
	//
	//   修改：@GetMapping("/booking/rsv/edit")
	//     參數：@RequestParam(required = false) Long rsvNo，以及 date、meal、keyword
	//     用 rsvNo 查出那一筆放進 rsvForm；查不到、或狀態已經是「取消」→ 帶訊息 redirect 回 /staff/booking。
	//     formDate 用那筆訂位的日期：rsvDt.getRsvDtDatetime().toLocalDate()
	//     formTime 用那筆訂位的時間，轉成「時:分」的文字：
	//         rsvDt.getRsvDtDatetime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
	//     ★ 如果這筆訂位原本的時間不是那四個時段之一（例如舊的資料是 18:30），對話框的時段會沒有一個被選起來，
	//       使用者要重新選一個才能儲存。這是時段寫死的限制，知道有這件事就好。
	//   兩個方法都 return "staff/booking";
	//
	//   ★ 之後想改成「時段從資料庫拿」（不同日子可以訂的時段不一樣、額滿的不顯示）的做法：
	//       ・這裡多放 rsvSlots：ReservationDatetimeService.getAvailableByDate(formDate)（List<ReservationDatetimeVO>）
	//       ・booking.html 的時段改成用 th:each 把 rsvSlots 產生出來，送出的值從時間的文字（rsvTime）換成時段編號（rsvDtNo）
	//       ・時段跟著日期變，而我們不用 JSON，所以在對話框換了日期要「重新載入頁面」才拿得到那一天的時段：
	//         月曆加上 data-date-submit，選了日期自動用 GET 把表單目前填的欄位送到 /staff/booking/rsv/add，
	//         這個方法再把使用者已經填的內容放回 rsvForm。（月曆的 data-date-submit 怎麼用，寫在 booking.js 的 A-2）
	//     這是比較大的修改，等四個固定時段的版本做完、確定需要再改。
	//
	//
	// ══ 步驟 3：儲存訂位（新增一個方法）══
	//   @PostMapping("/booking/rsv/save")
	//   表單送來的欄位（都是 required = false）：
	//     Long rsvNo           新增時是空的，修改時有值
	//     String memName       姓名（必填，最多 30 個字）
	//     String memTel        電話（必填，10 個數字）
	//     LocalDate formDate   日期（必填）
	//     String rsvTime       時段（必填），例如 "17:00"
	//     Integer seatTypeNo   桌型編號（必填）
	//     String rsvStatus     狀態（修改時才有；新增固定是「預約」）
	//     String rsvComment    備註（最多 50 個字）
	//     date、meal、keyword  列表目前的查詢條件（不通過、要再顯示頁面時，後面的列表用）
	//   另外還要 Model model、RedirectAttributes redirectAttributes。
	//   要做的事：
	//     ・檢查：必填、電話的格式、rsvTime 是不是那四個時段之一（規定 B）、桌型真的存在、新增時不能訂今天以前的日期。
	//     ・★ 找訂位時段：訂位表存的是「訂位時段編號」，所以要把日期和時間合成一個時間，去訂位時段表找那一筆：
	//         LocalDateTime dateTime = formDate.atTime(LocalTime.parse(rsvTime));   // 那一天的 17:00
	//       再用 dateTime 去找 ReservationDatetimeVO（ReservationDatetimeRepository 有沒有「用時間查」的方法要確認，沒有的話要加）。
	//       找不到的時候要怎麼辦，請和負責訂位的組員討論，兩種做法：
	//         (甲) 自動新增一筆那個時間的訂位時段，再用它的編號（後台新增最方便，但要確認不會和組員建立時段的做法衝突）
	//         (乙) 回傳錯誤「這個時段沒有開放訂位」（那就要先在資料庫把每一天的四個時段建好）
	//     ・★ 會員：訂位表存的是會員編號，不是姓名電話。用電話去會員表找：
	//         找得到 → 用那個會員；找不到 → 建立一筆新的會員（只有電話和姓名），再用它的編號。
	//       這是之前討論決定的做法，這一段由組員撰寫（MemberRepository 目前有沒有「用電話查」的方法要確認）。
	//       要注意：範例資料裡有好幾個會員是同一支電話，「用電話查」可能查到不只一筆，要決定用哪一筆。
	//     ・★ 名額：同一個時段、同一種桌型能訂幾桌是有限的（seat_type.seat_type_rsv_num 是開放訂位的桌數）。
	//       組員的 ReservationService.addForMember 已經有檢查名額的做法（裡面還有鎖定，避免兩個人同時訂到最後一桌），
	//       後台新增時建議直接呼叫它，不要自己另外寫一套。
	//     ・修改：先用 rsvNo 查出原本那一筆，改它的欄位再存，不要 new 一個新的來存。
	//       換了時段或桌型的話，一樣要檢查新的時段還有沒有名額。
	//     ・★ 狀態（規定 D）：這一頁只能把狀態設成「預約」或「未到」。
	//         原本那一筆已經是「實到」→ 不要改它的狀態（畫面上會送回「實到」，照原本的留著就好）
	//         原本不是「實到」，送來的卻是「實到」→ 不接受（有人自己改了表單），當作不通過
	//         送來的是「取消」→ 也不接受；要取消請走步驟 4
	//   通過：redirectAttributes.addFlashAttribute("message", "已新增訂位");   ← 修改時換成「已修改訂位」
	//         回到那筆訂位的那一天、那個餐期，才看得到剛剛存的：
	//           redirectAttributes.addAttribute("date", formDate);
	//           redirectAttributes.addAttribute("meal", 那個時間在 15:30 以前 ? "lunch" : "dinner");
	//           return "redirect:/staff/booking";
	//   不通過：model.addAttribute("errorMsg", "錯誤訊息");
	//           和步驟 2 一樣放 rsvForm、formDate、formTime（就是送來的 rsvTime）、seatTypes，以及後面的列表（用送來的 date、meal、keyword 查），
	//           return "staff/booking";   ← 對話框會再出現並顯示錯誤訊息
	//           ★ rsvForm 要裝著使用者剛剛填的內容，欄位才不會被清空。姓名和電話在 VO 裡是放在 member 底下，
	//             所以要 new 一個 MemberVO、設定姓名電話、再 setMember 進去；桌型同理（new SeatTypeVO、setSeatTypeNo）。
	//             修改的話記得把 rsvNo 也設回去，畫面才會維持「修改訂位」。這個物件只是給畫面顯示用，不要拿去存檔。
	//
	//
	// ══ 步驟 4：取消訂位（新增一個方法）══
	//   ★ 不是真的刪除資料：把這筆訂位的狀態（rsvStatus）改成「取消」，資料留在資料庫裡。
	//     訂位是有紀錄意義的資料（之後要查某位客人訂過什麼、有沒有常常取消），所以不刪。
	//   @PostMapping("/booking/rsv/cancel")
	//   參數：@RequestParam(required = false) Long rsvNo，以及 date、meal、keyword（做完回到原本的列表用），RedirectAttributes redirectAttributes
	//   要做的事：
	//     ・用 rsvNo 查出那一筆；查不到 → 帶訊息（例如「查無此訂位」）redirect 回去。
	//     ・已經是「取消」的 → 不用再改，帶訊息回去就好。
	//     ・已經是「實到」的 → 不能取消（客人已經入座了），帶訊息回去。畫面上實到的訂位沒有這顆按鈕，但表單可以自己送，所以要檢查。
	//     ・其他 → 把 rsvStatus 設成「取消」再存檔（先查出來、改欄位、再存，不要 new 一個新的來存）。
	//       狀態的欄位是 VARCHAR(2)，「取消」剛好兩個字，放得下。
	//     ・帶「已取消訂位」的訊息，redirect 回 /staff/booking（把 date、meal、keyword 用 redirectAttributes.addAttribute 放回網址）。
	//   畫面上按「取消訂位」會先跳出確認的視窗（staff.js 的 A-6），使用者按確定才會送到這裡。
	//   要注意：
	//     ・取消之後這筆訂位就不會出現在列表上了（規定 C），這一頁也沒有辦法把它改回來。
	//       真的取消錯了，就重新新增一筆；之後如果需要「看得到取消的訂位、可以恢復」，再另外加功能。
	//     ・★ 名額：組員算「這個時段還剩幾桌可以訂」的時候，有沒有把「取消」的訂位排除掉要確認。
	//       沒有排除的話，取消之後那一桌還是會被算成已經被訂走，別人訂不到。
	//     ・ReservationService 目前沒有「改狀態」的方法，要請負責訂位的組員加，或自己在這裡用 Repository 寫。
	//
	//
	// ══ 步驟 5：候位的查詢（下面 booking 方法裡 tab 是 "waiting" 的那一支）══
	//   放進 Model：
	//     tab          "waiting"
	//     waitingList  今天的候位（List<WaitingVO>），排除狀態是「取消」的（規定 C）。
	//                  建議的順序：還在處理中的（已通知、候位中）排前面，再依桌型、候位號碼排
	//     seatBoards   每種桌型一格（List）。每一格要有這五個屬性（用 Map 或另外寫一個小類別都可以）：
	//                    seatTypeNo     桌型編號（幾人桌）
	//                    pending        目前可以使用的桌數（這一頁只顯示；數字由入座管理更新，規定 D）
	//                    waitingCount   還在等的組數 ＝ 狀態是「候位中」的筆數（WaitingService.countActiveBySeatType 可以參考）
	//                    callingNos     叫號中的號碼（List<Long>；可以不只一個，沒有的話放空的清單）
	//                                   ＝ 這個桌型裡狀態是「已通知」的候位號碼
	//                    lateNos        callingNos 裡已經逾時的號碼（List<Long>；沒有的話也要放空的清單，不能不放）
	//                                   ＝ 已通知、而且通知時間加 10 分鐘已經比現在早：
	//                                      w.getWaitingNotifyTime().plusMinutes(10).isBefore(LocalTime.now())
	//                                   畫面上已叫號的號碼是紅底，逾時的會變成綠底，提醒工作人員可以按「過號」了
	//   ★ callingNos 和 lateNos 裡的號碼型別要一樣（都是 Long，或都是 Integer）。
	//     頁面是用「lateNos 裡有沒有這個號碼」來判斷逾時，型別不一樣的話會永遠比不到。
	//   ★ 候位表沒有日期的欄位，「今天的候位」怎麼判斷要先和組員確認
	//     （之前討論過的做法：每天固定時間把候位重設，這樣表裡的就都是今天的）。
	//   ★ Redis：取號、入座號、目前可用桌數之後預計改接 Redis。到時候 pending、callingNos 從 Redis 拿，
	//     這個方法放進 Model 的名稱不用變，頁面就不用改。現在可以先用 seat_type 表的 seat_type_pending 頂著。
	//   ★ 候位號碼：候位表預計加一個「候位號碼」的欄位（每種桌型各自編號）。加了之後：
	//       ・callingNos、lateNos 放的是那個欄位的值
	//       ・booking.html 列表的「號碼」那一欄，把 w.waitingNo 換成新的屬性（那裡有 ★ 標記）
	//     還沒加之前，可以先用候位編號（waitingNo）當號碼。
	//   ★ 列表上不顯示電話（waitingTel），但查詢不用特別處理：頁面沒有用到那個屬性就不會顯示。
	//
	//
	// ══ 步驟 6：新增／修改候位的對話框（新增兩個方法）══
	//   做法和訂位的步驟 2 相同：對話框靠 Model 裡有 waitingForm 才會出現。
	//   兩個方法都要先做步驟 5 的事（後面的列表和叫號才會在），再多放：
	//     waitingForm  要填的候位（WaitingVO）
	//     seatTypes    所有桌型（List<SeatTypeVO>），給「桌型」的選單用
	//
	//   新增：@GetMapping("/booking/waiting/add")
	//     替不會用網路的客人取號。waitingForm 放 new WaitingVO()。
	//   修改：@GetMapping("/booking/waiting/edit")
	//     參數：@RequestParam(required = false) Long waitingNo
	//     用 waitingNo 查出那一筆放進 waitingForm（WaitingService.getOneWaiting）；
	//     查不到、或狀態已經是「取消」→ 帶訊息 redirect 回 /staff/booking?tab=waiting。
	//   兩個方法都 return "staff/booking";
	//   ★ 這兩個方法回傳的是候位的分頁，tab 要放 "waiting"。
	//
	//
	// ══ 步驟 7：儲存候位（新增一個方法）══
	//   @PostMapping("/booking/waiting/save")
	//   表單送來的欄位（都是 required = false）：
	//     Long waitingNo          新增時是空的，修改時有值
	//     String waitingName      姓名（必填，最多 30 個字）
	//     String waitingTel       電話（必填，10 個數字；叫號的簡訊會傳到這支電話）
	//     Integer seatTypeNo      桌型編號（必填）
	//     String waitingStatus    狀態（修改時才有；新增固定是「候位中」）
	//     String waitingComment   備註（最多 20 個字）
	//   另外還要 Model model、RedirectAttributes redirectAttributes。
	//
	//   新增（waitingNo 是 null）：
	//     WaitingService.addWaiting 已經有新增的方法（會把狀態設成「候位中」，也會擋同一支電話重複候位）。
	//     它的第一個參數是會員編號：現場的客人不一定是會員，可以傳 null（候位表的會員編號可以是空的）。
	//     ★ 候位號碼：新增時要替這個桌型取下一個號碼（現在是 seat_type.seat_type_take_num 加 1，之後改用 Redis）。
	//
	//   修改（waitingNo 有值）：先用 waitingNo 查出原本那一筆，改它的欄位再存，不要 new 一個新的來存。
	//     ・★ 狀態（規定 D）：這一頁可以設成 候位中、已通知、過號、取消 四種。
	//         原本那一筆已經是「已入座」→ 不要改它的狀態（畫面上會送回「已入座」，照原本的留著就好）
	//         原本不是「已入座」，送來的卻是「已入座」→ 不接受，當作不通過
	//     ・狀態改成「已通知」：這裡只是改資料，不會傳簡訊（要通知客人請用步驟 9 的叫號）。
	//       原本沒有通知時間的話，建議把通知時間設成現在，逾時才算得出來；改回「候位中」的話把通知時間清掉。
	//     ・狀態改成「取消」：存檔之後這一筆就不會出現在列表上了（規定 C）。
	//     ・組員的 WaitingService 改狀態的方法（notifyWaiting、missWaiting、cancelByStaff）各自會檢查「原本的狀態對不對」，
	//       例如 missWaiting 只接受已通知的。編輯是讓工作人員「更正」用的，可能會遇到那些方法不允許的改法
	//       （例如把過號的改回候位中）。要直接用 Repository 存，還是請組員加一個「後台更正狀態」的方法，請和負責候位的組員討論。
	//     ・★ 換了桌型：候位號碼是每種桌型各自編的，換桌型之後原本的號碼就不對了。請和組員討論要怎麼做：
	//         (甲) 在新的桌型重新取一個號碼（假資料是這樣做的；客人等於重新排隊）
	//         (乙) 修改時不准換桌型（要換就取消再重新取號）。選這個的話，booking.html 修改時把桌型的選單鎖起來
	//     ・同一支電話在同一種桌型不能重複候位：修改的時候檢查要排除自己這一筆，不然會被自己擋住。
	//
	//   通過：帶 message（新增：「已新增候位：2 人桌 13 號」，方便工作人員把號碼告訴客人；修改：「已修改候位」），
	//         redirectAttributes.addAttribute("tab", "waiting");  return "redirect:/staff/booking";
	//   不通過：和步驟 6 一樣放列表的資料、seatTypes，再多放
	//           waitingError  錯誤訊息
	//           waitingForm   裝著使用者填的內容（WaitingVO），欄位才不會被清空；修改的話記得把 waitingNo 設回去
	//           return "staff/booking";
	//   ★ WaitingService 的方法遇到不合規定的資料是「丟出例外」（IllegalArgumentException），不是回傳訊息。
	//     這裡要用 try / catch 接住，把例外的訊息（e.getMessage()）當成 waitingError 顯示；
	//     沒有接的話使用者會看到錯誤頁。
	//
	//
	// ══ 步驟 8：過號（新增一個方法）══
	//   已通知的客人沒有在 10 分鐘內回來。@PostMapping("/booking/waiting/miss")
	//   參數：@RequestParam(required = false) Long waitingNo、RedirectAttributes redirectAttributes
	//   呼叫 WaitingService.missWaiting(waitingNo)（已通知 → 過號），做完帶訊息 redirect 回 /staff/booking?tab=waiting。
	//   狀態不對（不是已通知）的時候它會丟出例外，一樣要用 try / catch 接住，把訊息帶回頁面。
	//   畫面上只有「已通知」的那幾列有這顆按鈕，按下去會先跳出確認的視窗（staff.js 的 A-6）。
	//   ★ 10 分鐘的過號：目前是工作人員看到號碼變成綠底（逾時），確認客人沒回來，自己按「過號」。
	//     想要時間到自動過號的話，要另外寫排程（@Scheduled），那是之後的事。
	//   ★ 這一頁沒有「通知」「入座」「取消」的按鈕：
	//       通知 → 用步驟 9 的叫號（照號碼順序）　入座 → 入座管理負責（規定 D）　取消 → 在編輯的對話框把狀態改成取消（步驟 7）
	//
	//
	// ══ 步驟 9：叫下一號（新增一個方法）══
	//   右邊每種桌型那張小卡的「叫下一號」。@PostMapping("/booking/waiting/call")
	//   參數：@RequestParam(required = false) Integer seatTypeNo、RedirectAttributes redirectAttributes
	//   要做的事：找出這個桌型狀態是「候位中」、號碼最小的那一位，通知他：
	//     WaitingService.getActiveBySeatType(seatTypeNo) 會回傳這個桌型還在等的候位（已經依編號排好），
	//     裡面第一筆狀態是「候位中」的就是下一位；對他呼叫 WaitingService.notifyWaiting(waitingNo)
	//     （候位中 → 已通知，記下通知時間），★ 並傳簡訊（見下面）。
	//   沒有候位中的客人 → 帶訊息「2 人桌目前沒有候位中的客人」redirect 回去，不要當成錯誤。
	//   叫了下一號之後，那張小卡的「叫號中」會多一個號碼；
	//   那位客人入座（入座管理改的）、過號或取消之後，他的號碼就會從「叫號中」消失。所以同時可能有好幾個號碼。
	//
	//   ★ 通知要傳的簡訊內容（和 booking.html 上給工作人員看的說明要一致）：
	//       「您的候位已到號，請在 10 分鐘內回到店門口；逾時未到將視為過號，過號需重新填寫候位表單。」
	//     傳到 waitingTel 那支電話。傳簡訊要另外找簡訊服務（需要申請帳號、會收費），程式裡會用到帳號密碼：
	//       ・帳號密碼不要直接寫在程式裡，放在 application.properties（那個檔案不會上傳到 GitHub）。
	//       ・建議把「傳簡訊」寫成一個獨立的類別（例如 SmsService），這裡只呼叫它。
	//         簡訊服務還沒申請之前，先讓它只印出要傳的內容（System.out.println），其他功能就可以先做、先測。
	//       ・簡訊傳失敗時要怎麼辦要先決定：建議狀態照樣改成已通知，但在訊息裡告訴工作人員「簡訊沒有傳成功，請改用電話通知」。
	//   ★ Redis：之後「入座號」改放 Redis 的話，這裡通知完要一起更新 Redis 裡的數字。
	//
	//
	// ══ 全部做完之後 ══
	//   確認資料庫的畫面沒問題後，照 booking.html 開頭的步驟 10 刪掉假資料（HTML 5 段、booking.js 的 B 區）。

	// 步驟 1、5：打開「訂候位」的頁面。目前只告訴頁面現在是哪一個分頁，資料還是假資料
	@GetMapping("/booking")
	public String booking(@RequestParam(required = false) String tab, Model model) {
		// tab 只會是 "rsv"（訂位）或 "waiting"（候位）；網址沒有帶、或帶了別的字，都當作訂位
		model.addAttribute("tab", "waiting".equals(tab) ? "waiting" : "rsv");
		return "staff/booking";
	}
}
