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
//       活動管理    Model 有 promotes（「活動詳細」的對話框另外要有 promoteDetail；新增／修改的對話框另外要有 promoteForm）
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
//   /staff/promote?status=&keyword=&page=   GET   活動管理。依狀態、活動名稱去資料庫查「第 page 頁」的 8 筆（依結束時間排序），
//                                              Model 放 promotes（那一頁的 List<PromoteVO>）、status、keyword、page、totalPages
//   /staff/promote/detail?promoteNo=   GET    （要新增）和 /staff/promote 相同，另外 Model 多放 promoteDetail（查出來的那一筆），頁面會打開「活動詳細」的對話框
//   /staff/promote/img?promoteNo=      GET    （要新增）輸出活動圖片的內容（回傳 ResponseEntity<byte[]>，不是頁面），給卡片上的圖片、「活動詳細」和修改表單的 <img> 顯示
//   /staff/promote/add                 GET    （要新增）和 /staff/promote 相同，另外 Model 多放 promoteForm（new PromoteVO()），頁面會打開新增的對話框
//   /staff/promote/edit?promoteNo=     GET    （要新增）同上，promoteForm 放從資料庫查出來的那一筆，頁面會打開修改的對話框
//   /staff/promote/save                POST   （要新增）收 promoteNo（新增時是空的）、promoteName、promoteBegin、promoteEnd、promoteContent、upImg（圖片檔案）：
//                                              promoteNo 是 null 就新增一筆，有值就修改那一筆；成功後帶 message redirect 回 /staff/promote
//                                              詳細的參數與寫法見這個檔案最下面 promote 方法上方的說明
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
//	@GetMapping("/orders")
//	public String orders() {
//		return "staff/orders";
//	}

	// 結帳
	// 【Thymeleaf 串接】加上 @RequestParam(required = false) String order 和 Model，
	//   放入未結帳的訂單清單；有帶 order 時，把那一筆也放進去（見 checkout.html）
//	@GetMapping("/checkout")
//	public String checkout() {
//		return "staff/checkout";
//	}

	// 活動管理：查詢、新增、修改活動（資料表 promote，VO 是 com.bistroops.promote.model.PromoteVO）
	// 畫面上每個活動是一張卡片（上面圖片，下面名稱、期間、狀態、「詳細」），按「詳細」看全部的資訊，再從詳細裡按「修改」。
	// 目前只負責打開頁面，畫面上的資料是 static/js/staff/promote.js 裡的假資料。
	// 之後活動的功能有自己的 Controller 時，把這個方法和下面的說明一起搬過去（網址維持 /staff/promote 就不用改 HTML）。
	//
	// 【Thymeleaf 串接】promote.html 裡【資料庫資料】的那一份已經用 Thymeleaf 寫好了，
	//   這裡把資料放進 Model 之後，它就會和假資料同時出現在頁面上。
	//
	//   ★★ 寫 Java 之前先看：容易出錯的地方 ★★
	//     (1) 先改 PromoteVO：資料表已經沒有 promote_status 了，但 PromoteVO 還留著 promoteStatus。
	//         不拿掉的話，只要查詢活動就會出錯（Unknown column 'promote_status'），頁面完全打不開。
	//         要拿掉的有：@Column(name="promote_status") 和 private String promoteStatus、getter、setter，
	//         還有 toString() 裡面用到 promoteStatus 的那一段。
	//     (2) 六個方法（查詢、detail、add、edit、save、img）裡，回傳 "staff/promote" 的每一個
	//         都要把 promotes、status、keyword、page、totalPages 放進 Model，不然對話框後面的卡片和分頁會整塊不見。
	//         建議寫一個自己用的方法，每個方法都呼叫它，例如
	//           private void putList(Model model, String status, String keyword, Integer page)
	//         查詢、算頁數、放進 Model 都寫在這個方法裡（內容就是下面步驟 1）。
	//     (3) 查不到資料要放「空的清單」，不要放 null（null 會讓資料庫那一份整塊不出現）。
	//     (4) save 的參數建議都加 required = false，再自己用 if 檢查。
	//         原因：少了必填的參數，或時間的格式不對，Spring 會直接顯示 400 錯誤頁，使用者看不到我們寫的錯誤訊息。
	//     (5) ★ 日期：表單送來的開始、結束只有「年月日」（文字長這樣：2026-10-15），沒有幾點幾分。
	//         所以不能直接用 LocalDateTime 接（會轉換失敗、變成 400 錯誤頁）。
	//         要用「只有日期」的型別（LocalDate）接，並用 @DateTimeFormat 指定格式是 年-月-日；
	//         接到之後再由 Java 補上時間，變成 LocalDateTime 才放進 PromoteVO。完整的規則見下面的「日期與時間的規則」。
	//     (6) 修改時不要 new 一個新的 PromoteVO 來存：先用 promoteNo 查出原本那一筆，改它的欄位再存。
	//         new 一個新的來存，沒有設定到的欄位（例如圖片）會被存成 null，原本的圖片就不見了。
	//     (7) 檢查不通過、要回到表單時，使用者剛剛選的圖片檔案會不見（瀏覽器不能幫忙填回檔案欄位），要請他重選。
	//         修改的情況，放回 promoteForm 的那個物件記得帶著原本的圖片（或直接用查出來的那一筆、把文字欄位換成使用者填的），
	//         表單的預覽才會顯示目前的圖片，而不是「尚無圖片」。
	//     (8) 圖片大小：application.properties 裡 multipart 的設定目前是註解掉的，所以用預設值，單一檔案最多 1MB。
	//         超過會直接出錯。要放寬的話把那兩行的 # 拿掉並改數字，例如
	//           spring.servlet.multipart.max-file-size=5MB
	//           spring.servlet.multipart.max-request-size=10MB
	//     (9) 輸出圖片的方法可以參考公告管理已經寫好的 AnnouncementController 的 image 方法，寫法幾乎一樣。
	//     (10) 「套用已有活動」（步驟 7）放到最後做，先不要管它；其他功能都不需要它。
	//     (11) 需要的 import（少了會出現紅線）：
	//           org.springframework.ui.Model
	//           org.springframework.web.bind.annotation.RequestParam、PostMapping、ResponseBody
	//           org.springframework.format.annotation.DateTimeFormat
	//           org.springframework.web.multipart.MultipartFile
	//           org.springframework.web.servlet.mvc.support.RedirectAttributes
	//           org.springframework.http.ResponseEntity、MediaType、HttpHeaders
	//           java.time.LocalDate、java.time.LocalDateTime、java.io.IOException、java.util.List
	//           com.bistroops.promote.model.PromoteVO
	//         另外還需要活動的 Repository／Service（目前 promote 只有 PromoteVO，這兩個要自己新增），
	//         寫法可以參考 announcement 資料夾裡的 AnnouncementRepository 和 AnnouncementService。
	//
	//   ★★ 日期與時間的規則（畫面上已經擋了，但畫面的檢查可以被跳過，Java 一定要再做一次）★★
	//     畫面只讓使用者選「年月日」，也只顯示年月日；資料表存的還是完整的時間，幾點幾分由 Java 決定：
	//       開始時間 ＝ 使用者選的開始日期 的 00:00
	//       結束時間 ＝ 使用者選的結束日期 的 23:59
	//     例如開始選 10/15、結束選 10/16，存進去的就是 10/15 00:00 到 10/16 23:59。
	//
	//     存檔前要檢查的事（新增和修改都要）：
	//       a. 開始日期、結束日期都要有。
	//       b. 開始日期不能早於今天。（例外：進行中的活動，見 d）
	//       c. 結束日期不能早於今天，而且最早是開始日期的「隔天」（不能和開始日期同一天，也不能更早）。
	//          畫面上（promote.js）用的是同樣的規則，兩邊要一樣。
	//       d. 進行中的活動「不能修改開始時間」：
	//            畫面上開始日期是鎖住的，但表單還是會把它送過來，而且那個值可以被改。
	//            所以修改時，先查出資料庫原本那一筆，用「原本的時間」判斷它是不是進行中；
	//            是的話，不管表單送什麼開始日期，都保留原本的開始時間，只更新結束時間和其他欄位。
	//       e. 已經結束的活動不能修改（一樣用資料庫原本的時間判斷）。
	//     判斷「今天」要用伺服器的日期，不要相信表單送來的任何「現在是幾號」。
	//
	//     顯示：畫面一律只顯示年月日（promote.html 已經用只有年月日的格式轉好了），Java 不用另外處理。
	//           放進 Model 的還是 PromoteVO 原本的 LocalDateTime，不要先轉成文字。
	//
	//     查詢與狀態不受影響：未開始、進行中、已結束還是用「完整的時間」和現在比
	//     （結束時間是 23:59，所以活動在結束日期當天整天都算進行中，隔天才變成已結束）。
	//
	//   要做的事（依照順序）：
//
	//   1. 查詢（改下面這個方法）
	//        ★★ 搜尋、狀態、分頁、排序全部由 Java 去資料庫查，而且一次只查一頁（8 筆）★★
	//           畫面上按「搜尋」、換狀態、按上一頁／下一頁，都是帶著條件重新連到這個方法，查好再顯示。
	//           不要用 findAll() 把全部的活動查出來再用 Java 挑：活動一多，每次都要把所有資料（連圖片）讀出來，會很慢。
	//
	//        參數：@RequestParam(required = false) String status     狀態：空的＝全部／未開始／進行中／已結束
	//              @RequestParam(required = false) String keyword    關鍵字：活動名稱裡有這些字
	//              @RequestParam(required = false) Integer page      第幾頁（從 1 開始）
	//              Model model
	//
	//        (a) 先把參數整理好
	//              if (keyword == null) keyword = "";          // 沒有輸入就是空字串（空字串會符合每一筆）
	//              keyword = keyword.trim();
	//              if (page == null || page < 1) page = 1;     // 沒有帶頁數，或被改成奇怪的數字，都當作第 1 頁
	//
	//        (b) 準備「分頁＋排序」的條件（Spring Data 的 Pageable）
	//              Pageable pageable = PageRequest.of(page - 1, 8, Sort.by("promoteEnd").descending());
	//            三個參數的意思：
	//              page - 1   要第幾頁。★ Spring 的頁數是從 0 開始算的，所以畫面上的第 1 頁要傳 0，一定要減 1
	//              8          一頁幾筆
	//              Sort.by("promoteEnd").descending()   依結束時間排序，晚的排前面。
	//                         "promoteEnd" 是 PromoteVO 的屬性名稱（不是資料表的欄位名稱 promote_end）。
	//                         想改成早的排前面，把 descending() 換成 ascending()。
	//
	//        (c) 依狀態呼叫不同的查詢。狀態「不存在資料庫」，是用開始、結束時間和現在的時間比出來的，
	//            所以要換成時間的條件。在活動的 Repository（要繼承 JpaRepository<PromoteVO, Integer>）加上這四個方法，
	//            方法的名稱照著寫，Spring Data 就會自動產生對應的 SQL，不用自己寫查詢：
	//
	//              全部    Page<PromoteVO> findByPromoteNameContaining(String keyword, Pageable pageable);
	//              未開始  Page<PromoteVO> findByPromoteNameContainingAndPromoteBeginAfter(
	//                              String keyword, LocalDateTime now, Pageable pageable);
	//              進行中  Page<PromoteVO> findByPromoteNameContainingAndPromoteBeginLessThanEqualAndPromoteEndGreaterThanEqual(
	//                              String keyword, LocalDateTime now1, LocalDateTime now2, Pageable pageable);
	//              已結束  Page<PromoteVO> findByPromoteNameContainingAndPromoteEndBefore(
	//                              String keyword, LocalDateTime now, Pageable pageable);
	//
	//            名稱的意思：Containing＝裡面有這些字、After＝晚於、Before＝早於、
	//                        LessThanEqual＝小於等於（不晚於）、GreaterThanEqual＝大於等於（不早於）。
	//            呼叫的時候：
	//              LocalDateTime now = LocalDateTime.now();
	//              Page<PromoteVO> result;
	//              if ("未開始".equals(status))      result = repository.findBy…BeginAfter(keyword, now, pageable);
	//              else if ("進行中".equals(status)) result = repository.findBy…GreaterThanEqual(keyword, now, now, pageable);   // now 傳兩次
	//              else if ("已結束".equals(status)) result = repository.findBy…EndBefore(keyword, now, pageable);
	//              else                              result = repository.findByPromoteNameContaining(keyword, pageable);
	//            （"未開始".equals(status) 這種把文字寫在前面的寫法，status 是 null 也不會出錯）
	//
	//        (d) 把結果放進 Model。Page 這個物件裡有這一頁的資料，也有總頁數、總筆數：
	//              model.addAttribute("promotes", result.getContent());                       // 這一頁的活動（最多 8 筆）
	//              model.addAttribute("totalPages", Math.max(1, result.getTotalPages()));     // 沒有資料時 getTotalPages() 是 0，畫面要顯示 1
	//              model.addAttribute("total", result.getTotalElements());                    // 符合條件的總筆數（可以不放）
	//              model.addAttribute("page", page);
	//              model.addAttribute("status", status);
	//              model.addAttribute("keyword", keyword);
	//            result.getContent() 查不到資料時是空的清單（不是 null），所以資料庫那一份還是會出現，並顯示「沒有符合條件的活動」。
	//
	//        要注意：
	//          ・頁數超過最後一頁（例如停在第 3 頁時資料被改到只剩 2 頁）：查出來會是空的。
	//            可以在 (c) 之後檢查，page > result.getTotalPages() 而且總頁數大於 0 時，把 page 改成最後一頁再查一次。
	//          ・畫面上每一筆顯示的狀態，promote.html 已經用時間的判斷式寫好了（th:with 裡的 statusText），這裡不用另外放。
	//          ・需要多 import：org.springframework.data.domain.Page、Pageable、PageRequest、Sort
	//
	//   2. 打開「活動詳細」的對話框（新增一個方法）
	//        每一張卡片的「詳細」會連到這裡：先顯示這筆活動全部的資訊，對話框下方才是「修改」。
	//        @GetMapping("/promote/detail")，參數 @RequestParam Integer promoteNo，
	//          以及和查詢一樣的 status、keyword、page（「詳細」的網址會帶著目前的條件，都是 required = false）
	//        和查詢一樣查出那一頁並放 promotes、status、keyword、page、totalPages（呼叫 putList），再把那一筆活動放進去：
	//        model.addAttribute("promoteDetail", 用 promoteNo 查出來的 PromoteVO);
	//        查不到那一筆（例如網址被改過）就 return "redirect:/staff/promote";
	//        return "staff/promote";
	//        （對話框裡顯示的狀態，promote.html 已經用時間的判斷式寫好了，這裡不用放）
	//
	//   3. 打開新增的對話框（新增一個方法）
	//        @GetMapping("/promote/add")，參數是和查詢一樣的 status、keyword、page（都是 required = false）
	//        和查詢一樣查出那一頁並放 promotes、status、keyword、page、totalPages（呼叫 putList），再多放一個空的活動：
	//        model.addAttribute("promoteForm", new PromoteVO());
	//        return "staff/promote";
	//
	//   4. 打開修改的對話框（新增一個方法）。「活動詳細」下方的「修改」會連到這裡
	//        @GetMapping("/promote/edit")，參數 @RequestParam Integer promoteNo，以及 status、keyword、page（都是 required = false）
	//        和查詢一樣查出那一頁並放 promotes、status、keyword、page、totalPages（呼叫 putList），再把那一筆活動放進去：
	//        model.addAttribute("promoteForm", 用 promoteNo 查出來的 PromoteVO);
	//        查不到那一筆（例如網址被改過）就 return "redirect:/staff/promote";
	//        ★ 已經結束的活動不能修改。畫面上「活動詳細」在狀態是已結束時不顯示「修改」，
	//          但網址可以自己輸入，所以這裡要再擋一次：查出來的活動如果已經結束
	//          （promote.getPromoteEnd() != null && LocalDateTime.now().isAfter(promote.getPromoteEnd())），
	//          就不要打開修改的表單，帶一個訊息回去：
	//            redirectAttributes.addFlashAttribute("message", "已結束的活動不能修改");
	//            return "redirect:/staff/promote";
	//        ★ 進行中的活動可以打開修改的表單，但不能改開始日期。
	//          這裡不用特別處理：promote.html 會自己用時間判斷這筆是不是進行中，是的話把開始日期鎖住並顯示說明。
	//          要放進 promoteForm 的，就是查出來的那一筆（開始、結束時間維持原本的 LocalDateTime）。
	//          真正要擋的地方在儲存（步驟 5）。
	//        return "staff/promote";
	//
	//   5. 儲存（新增一個方法）
	//        @PostMapping("/promote/save")，表單會送來這些參數（名稱和 PromoteVO 的屬性相同）：
	//          @RequestParam(required = false) Integer promoteNo        新增時是空的，修改時是活動編號
	//          @RequestParam(required = false) String promoteName        活動名稱，最多 10 個字（資料表是 VARCHAR(10)）
	//          promoteBegin   開始日期。★ 只有年月日（2026-10-15）：用 LocalDate 接，加上 required = false 和指定「年-月-日」格式的 @DateTimeFormat
	//          promoteEnd     結束日期。同上
	//          （沒有 promoteStatus：狀態是用時間判斷的，表單不會送，也不用存）
	//          @RequestParam(required = false) String promoteContent     活動內容
	//          @RequestParam(value = "upImg", required = false) MultipartFile upImg   活動圖片（沒有選檔案時 upImg.isEmpty() 是 true）
	//          另外還要 Model model（不通過時用）和 RedirectAttributes redirectAttributes（通過時用）
	//        （日期送過來的文字長這樣：2026-10-15，沒有幾點幾分；格式不對時 Spring 轉不出來，
	//          加了 required = false 的話會拿到 null，就可以用下面的檢查顯示自己的錯誤訊息）
	//        檢查：名稱不能是 null 或空白（promoteName == null || promoteName.isBlank()）、不能超過 10 個字；
	//              日期的部分照上面「日期與時間的規則」的 a～e。
	//        補時間：檢查通過之後，把開始日期補成那一天的 00:00、結束日期補成那一天的 23:59，再放進 PromoteVO。
	//                修改進行中的活動時，開始時間不要補、也不要換，保留資料庫原本的。
	//        建議：檢查和補時間寫在 PromoteService，這裡只負責把結果（成功，或錯誤訊息）顯示出來。
	//        ★ 修改（promoteNo 有值）的時候，先查出原本那一筆；它如果已經結束，就不要存，
	//          一樣帶「已結束的活動不能修改」的訊息 redirect 回 /staff/promote（理由同步驟 4：表單可以被直接送出）。
	//          注意是看「資料庫裡原本的結束時間」，不是表單送來的新時間，不然把時間改晚一點就能繞過去。
	//        通過：promoteNo 是 null → 新增一筆；有值 → 先查出原本那一筆再改欄位（這樣原本的圖片不會被清掉），然後存檔。
	//              redirectAttributes.addFlashAttribute("message", "已新增活動");   ← 修改時換成「已修改活動」
	//              return "redirect:/staff/promote";
	//        不通過：model.addAttribute("errorMsg", "錯誤訊息");
	//                把使用者填的內容裝成 PromoteVO 放回 promoteForm（promoteNo 也要放，畫面才知道是新增還是修改；
	//                修改時記得帶著原本的圖片，見上面注意事項 (7)），連同 promotes、status、keyword、page、totalPages 一起放進 Model
	//                （儲存的表單沒有送 status、keyword、page，這時呼叫 putList(model, null, null, 1) 顯示第 1 頁就可以了），
	//                return "staff/promote";   ← 對話框會再打開並顯示錯誤訊息
	//
	//   6. 活動圖片
	//      ★ 存進資料庫、從資料庫取出來的都是使用者上傳的「原本的圖片」，不用做任何處理：
	//          有選檔案（upImg != null && !upImg.isEmpty()）→ promote.setPromoteImg(upImg.getBytes());
	//          沒有選檔案 → 新增時不用設定（promoteImg 是 null）；修改時保留資料庫原本的圖片，不要清掉
	//        upImg.getBytes() 會丟出 IOException，save 方法要加上 throws IOException。
	//        畫面上圖片固定用 16:9 顯示（首頁輪播、卡片上的圖片、活動詳細、新增／修改的預覽都一樣），是 CSS 做的（object-fit: fill）：
	//        其他比例的圖片顯示時會被拉成 16:9（會變形），不留白、不裁切。和這裡怎麼存、怎麼取無關。
	//
	//      輸出圖片（新增一個方法）：卡片上的圖片、「活動詳細」和修改表單的預覽，都是用 <img src="/staff/promote/img?promoteNo=1"> 顯示圖片，
	//        @GetMapping("/promote/img")
	//        public ResponseEntity<byte[]> promoteImg(@RequestParam Integer promoteNo) {
	//            用 promoteNo 查出活動；查不到，或圖片是 null、長度是 0 → return ResponseEntity.notFound().build();
	//            return ResponseEntity.ok()
	//                    .contentType(MediaType.IMAGE_JPEG)
	//                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")   // 不要用瀏覽器暫存的舊圖，換了圖片才會馬上看到新的
	//                    .body(promote.getPromoteImg());
	//        }
	//        和公告管理 AnnouncementController 的 image 方法是同樣的寫法，可以對照著看。
	//        （種類寫 IMAGE_JPEG：使用者上傳 PNG 時瀏覽器的 <img> 一樣顯示得出來，所以不用另外判斷）
	//        這個方法回傳的是圖片的內容、不是頁面，所以回傳型別是 ResponseEntity<byte[]>，不要回傳 "staff/promote"。
	//        這個方法沒寫的話，有圖片的活動會顯示成破圖。
	//
	//      首頁的輪播之後要顯示活動圖片時：
	//        前台要另外寫一個輸出圖片的方法（網址放在 /bistroops 底下，例如 /bistroops/promote/img），
	//        不要讓客人的頁面去連 /staff 的網址（之後員工登入做了攔截，客人就看不到圖片了）。
	//        首頁的 Controller 把「進行中」的活動放進 Model，輪播用 th:each 產生，寫法在 frontend_index.html 輪播上方的說明。
	//
	//      效能提醒：查詢時每一筆活動都會把圖片（最多 16MB）一起從資料庫讀出來（分頁之後一次最多 8 筆）；卡片顯示圖片時，
	//        瀏覽器還會再對每一筆各連一次 /staff/promote/img 把原圖抓下來。活動不多的時候沒關係；
	//        之後如果覺得活動管理變慢，可以再研究查詢時不要讀圖片的寫法（例如另外寫一個只查文字欄位的查詢）。
	//
	//
	//      檔案大小：見上面注意事項 (8)。資料表的 MEDIUMBLOB 最多可以存 16MB。
	//
	//   ▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼
	//   7. ★★【最後再做】套用已有活動 ★★
	//      請等上面 1～6 都完成、資料庫的畫面確認沒問題之後，再回來做這一項。
	//      在那之前，資料庫那一份的「套用已有活動」按鈕只會跳出「尚未開放」，不影響其他功能。
	//   ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲
	//      功能：新增活動時，按「套用已有活動」→ 出現搜尋的畫面（一開始不列出任何活動）
	//            → 輸入活動名稱按「搜尋」→ 顯示符合的「已經結束」的活動（名稱和結束時間）
	//            → 按「套用」→ 回到新增的表單，活動名稱、活動內容、活動圖片都帶進來，再填時間就可以儲存。
	//            存起來的是一筆新的活動，不會改到原本那一個。
	//      畫面的樣子可以先操作假資料的版本。HTML 要怎麼改（按鈕、挑選的畫面、隱藏欄位、預覽）
	//      寫在 promote.html【資料庫資料】新增／修改表單裡，用 ▼▼▼▲▲▲ 框起來的那一段。
	//
	//      Controller 要改的地方（不用 JSON，都是在原本的方法上多收幾個參數）：
	//        (a) add 方法多收三個參數（都是 required = false）：
	//              String pick          有值代表要顯示挑選的清單
	//              String pickKeyword   挑選時搜尋的活動名稱
	//              Integer copyFrom     按了「套用」的那個活動編號
	//        (b) 有 pick → Model 一定要放 pickList 和 pickKeyword（挑選的畫面靠 pickList 不是 null 才會出現）：
	//              ★ pickKeyword 是 null 或空白（剛打開、還沒搜尋）→ 不要查資料庫，直接放空的清單：
	//                  model.addAttribute("pickList", List.of());
	//                畫面會顯示「請輸入活動名稱，按搜尋後才會列出已結束的活動」。不要一打開就把已結束的活動全部列出來。
	//              pickKeyword 有內容 → 查出「已經結束」而且名稱裡有 pickKeyword 的活動：
	//                  model.addAttribute("pickList", 查出來的清單);
	//              兩種情況都要 model.addAttribute("pickKeyword", pickKeyword);
	//            查詢用步驟 1 的「已結束」那個方法，一樣只拿前面幾筆，例如
	//              repository.findByPromoteNameContainingAndPromoteEndBefore(
	//                      pickKeyword.trim(), LocalDateTime.now(),
	//                      PageRequest.of(0, 20, Sort.by("promoteEnd").descending())).getContent()
	//        (c) 有 copyFrom → 查出那個活動。查不到，或它還沒結束（結束時間不早於現在），就當作沒有 copyFrom。
	//            new 一個新的 PromoteVO，只設定活動名稱和活動內容（不要設定編號、開始、結束時間、圖片），當作 promoteForm；
	//            開始、結束日期留空讓使用者自己選（原本的日期都已經過了，本來也不能用）；
	//            再 model.addAttribute("copyFrom", copyFrom);   ← 表單的隱藏欄位和預覽圖片要用
	//        (d) save 方法多收 @RequestParam(required = false) Integer copyFrom，決定圖片的順序：
	//              有上傳新的檔案                              → 用上傳的
	//              沒有上傳、是新增、而且 copyFrom 有值         → 查出那個活動，promote.setPromoteImg(來源.getPromoteImg())
	//              都沒有                                       → 沒有圖片（修改時是保留原本的）
	//      要注意：
	//        ・圖片不能靠表單帶：瀏覽器不允許程式把檔案放進「選擇檔案」的欄位，所以才用 copyFrom 記住來源、儲存時由 Java 複製。
	//        ・copyFrom 是從網址和表單來的，可以被改。save 裡要再確認一次那個活動真的已經結束，才複製它的圖片。
	//        ・去挑選、按套用都會重新載入頁面，使用者之前填的內容會不見，所以畫面上請他「先套用、再填時間」。
	//        ・儲存檢查不通過、回到表單時，要把 copyFrom 再放回 Model，不然套用的圖片會不見。
	//
	//   1～6 全部做好、確認資料庫的畫面沒問題後，照 promote.html 開頭的步驟刪掉假資料
	//   （第 7 項可以在刪掉假資料之前或之後做；之後做的話，假資料的樣子就看不到了，建議先做）。
	@GetMapping("/promote")
	public String promote() {
		return "staff/promote";
	}

}
