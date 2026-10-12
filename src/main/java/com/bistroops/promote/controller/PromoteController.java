package com.bistroops.promote.controller;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bistroops.promote.model.PromoteService;
import com.bistroops.promote.model.PromoteVO;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/staff")
public class PromoteController {


	// ══════════════════════════════════════════════════════════════════════════════
	// 活動管理：查詢、新增、修改活動（資料表 promote，VO 是 com.bistroops.promote.model.PromoteVO）
	// ══════════════════════════════════════════════════════════════════════════════
	//
	// ── 活動管理的說明放在哪裡（每件事只寫在一個地方，串接從這個檔案開始看）──
	//   PromoteController.java   （這個檔案）有哪些網址、每個方法收什麼參數、放什麼進 Model、回傳哪一頁
	//   PromoteService.java      規則：日期與時間、能不能修改、圖片怎麼存；還沒寫的功能清單
	//   PromoteRepository.java   查詢怎麼寫；★ 之後的優化「列表不讀圖片（回傳小物件）」
	//   promote_list.html        列表頁：畫面用到哪些 Model 的名稱和欄位；刪除假資料的步驟
	//   promote_form.html        表單頁：表單送出哪些欄位；「套用已有活動」HTML 要怎麼改
	//   promote.js／promote.css  畫面上的檢查、日期選擇、圖片預覽、樣式（串接時不用改）
	//
	// ── 頁面 ──
	//   活動管理有兩個頁面，都在 templates/staff/promote/ 資料夾，所以回傳的名稱前面要加 promote/：
	//     "staff/promote/promote_list"   列表頁：查詢（狀態、搜尋、分頁）＋ 卡片 ＋「活動詳細」的對話框
	//     "staff/promote/promote_form"   表單頁：新增／修改活動（共用同一頁）
	//   兩個 HTML 裡【資料庫資料】的那一份已經用 Thymeleaf 寫好了：這裡把資料放進 Model，
	//   它就會和【假資料】同時出現在頁面上，可以對照著看。
	//
	// ── 目前的進度 ──
	//   ✔ 步驟 1 查詢、步驟 2 活動詳細、步驟 3 新增的表單、步驟 4 修改的表單：已經寫好，寫法沒有問題。
	//   ・ 還沒寫：步驟 6 輸出圖片（沒寫的話有圖片的活動會顯示成破圖）、步驟 5 儲存、步驟 9 刪除。
	//   ・ 可以整理的地方（不影響功能）：步驟 1 和步驟 2 有一大段一樣的程式（整理頁數、查詢、放進 Model）。
	//     可以搬成一個自己用的方法（注意事項 (3) 的 putList），兩邊各呼叫一行，之後要改只要改一個地方。
	//   PromoteService 的進度和要修正的地方，寫在 PromoteService.java 開頭的「目前的狀態」。
	//
	// ── 總覽：最後會有七個方法 ──
	//   步驟  網址                        回傳                              要放進 Model
	//    1    GET  /staff/promote         "staff/promote/promote_list"      promotes、status、keyword、page、totalPages（total、message 可以不放）
	//    2    GET  /staff/promote/detail  "staff/promote/promote_list"      同上，再加 promoteDetail
	//    3    GET  /staff/promote/add     "staff/promote/promote_form"      promoteForm（空的活動）
	//    4    GET  /staff/promote/edit    "staff/promote/promote_form"      promoteForm（查出來的那一筆）
	//    5    POST /staff/promote/save    成功   "redirect:/staff/promote"         用 RedirectAttributes 帶 message
	//                                     不通過 "staff/promote/promote_form"      promoteForm、errorMsg
	//    6    GET  /staff/promote/img     圖片的內容（不是頁面）            不用放
	//    9    POST /staff/promote/delete  "redirect:/staff/promote"         用 RedirectAttributes 帶 message
	//   只有步驟 1、2 要查列表。3、4、5、9 都不用查列表
	//   （表單頁的「返回活動列表」是 HTML 自己從網址讀 status、keyword、page）。
	//
	//   每種狀態能做的事（畫面上會把不能按的按鈕藏起來，但 Java 一定要自己再擋一次）：
	//     未開始 → 可以修改、可以刪除
	//     進行中 → 可以修改（不能改開始日期）、不能刪除
	//     已結束 → 不能修改、不能刪除，只能看
	//
	//   建議的順序：1 查詢 → 6 圖片 → 2 詳細 → 3 新增 → 5 儲存 → 4 修改 → 9 刪除 → 7 套用已有活動 → 8 優化
	//   每做完一個就打開頁面看【資料庫資料】有沒有出現，不要全部寫完才測。
	//
	//
	// ══ 寫之前先看：容易出錯的地方 ══
	//   (1) 先拿到 PromoteService：加一個欄位並標上 @Autowired（寫法和 PromoteService 裡拿 PromoteRepository 一樣），
	//       每個方法都透過它查資料、存資料，不要直接用 Repository。
	//   (2) 回傳的頁面名稱要寫完整（見上面「頁面」）。寫成舊的 "staff/promote" 會找不到頁面（TemplateInputException）。
	//   (3) 步驟 1、2 都要把 promotes、status、keyword、page、totalPages 放進 Model，不然卡片和分頁會整塊不見。
	//       建議寫一個自己用的方法，兩個步驟都呼叫它，例如
	//         private void putList(Model model, String status, String keyword, Integer page)
	//       內容就是步驟 1 的 (a)～(c)。
	//   (4) 查不到資料要放「空的清單」，不要放 null（null 會讓資料庫那一份整塊不出現）。
	//   (5) 查詢、詳細、修改用 @RequestParam 接的參數：status、keyword、page 都加 required = false。
	//       沒有加的話，只要少了一個參數，Spring 會直接顯示 400 錯誤頁，使用者看不到我們寫的錯誤訊息。
	//   (6) ★ 儲存用老師範例的寫法：@Valid PromoteVO 加 BindingResult（步驟 5）。
	//       表單送來的開始、結束是「完整的時間」，文字長這樣：2026-10-15T00:00、2026-10-16T23:59
	//       （畫面上使用者只選日期，幾點幾分是 promote.js 補的），所以可以直接裝進 PromoteVO 的 LocalDateTime。
	//       要先在 PromoteVO 補上檢查用的標註，@Valid 才有東西可以檢查（見步驟 5 的「PromoteVO 要準備的標註」）。
	//   (7) 修改時不要 new 一個新的 PromoteVO 來存：先用 promoteNo 查出原本那一筆，改它的欄位再存。
	//       new 一個新的來存，沒有設定到的欄位（例如圖片）會被存成 null，原本的圖片就不見了。
	//   (8) @Valid 只能檢查「單一欄位」的規則（必填、長度）。要比對兩個欄位或要查資料庫的規則它做不到，
	//       還是要自己寫（建議寫在 PromoteService）：開始日期不能早於今天、結束日期最早是開始日期的隔天、
	//       進行中不能改開始時間、已結束不能修改。
	//   (9) 需要的 import（少了會出現紅線）：
	//         org.springframework.beans.factory.annotation.Autowired
	//         org.springframework.ui.Model
	//         org.springframework.web.bind.annotation.RequestParam、PostMapping、ModelAttribute
	//         org.springframework.validation.BindingResult
	//         jakarta.validation.Valid
	//         org.springframework.web.multipart.MultipartFile
	//         org.springframework.web.servlet.mvc.support.RedirectAttributes
	//         org.springframework.http.ResponseEntity、MediaType、HttpHeaders
	//         org.springframework.data.domain.Page
	//         java.time.LocalDateTime、java.io.IOException
	//         com.bistroops.promote.model.PromoteVO、PromoteService
	//
	//
	// ══ 步驟 1：查詢（改下面的 promote 方法）　頁面：promote_list.html ══
	//   ★ 搜尋、狀態、分頁、排序全部由 Java 去資料庫查，一次只查一頁（8 筆）。
	//     畫面上按「搜尋」、換狀態、按上一頁／下一頁，都是帶著條件重新連到這個方法，查好再顯示。
	//
	//   參數：@RequestParam(required = false) String status     狀態：空的＝全部／未開始／進行中／已結束
	//         @RequestParam(required = false) String keyword    關鍵字：活動名稱裡有這些字
	//         @RequestParam(required = false) Integer page      第幾頁（從 1 開始）
	//         Model model
	//
	//   (a) 整理頁數：if (page == null || page < 1) page = 1;
	//       （關鍵字是 null、前後有空白的情況，PromoteService 的 getPromoteByStatus 裡面已經處理了）
	//
	//   (b) 呼叫 PromoteService 的 getPromoteByStatus(status, keyword, now, page)，它會依狀態選對應的查詢，
	//       回傳 Page<PromoteVO>。now 是 LocalDateTime.now()。
	//
	//   (c) 把結果放進 Model。Page 這個物件裡有這一頁的資料，也有總頁數、總筆數：
	//         model.addAttribute("promotes", result.getContent());                       // 這一頁的活動（最多 8 筆）；查不到時是空的清單，不是 null
	//         model.addAttribute("totalPages", Math.max(1, result.getTotalPages()));     // 沒有資料時 getTotalPages() 是 0，畫面要顯示 1
	//         model.addAttribute("total", result.getTotalElements());                    // 符合條件的總筆數（可以不放）
	//         model.addAttribute("page", page);
	//         model.addAttribute("status", status);
	//         model.addAttribute("keyword", keyword);
	//
	//   (d) return "staff/promote/promote_list";
	//
	//   要注意：
	//     ・頁數超過最後一頁（例如停在第 3 頁時資料被改到只剩 2 頁）：查出來會是空的。
	//       可以在 (b) 之後檢查，page > result.getTotalPages() 而且總頁數大於 0 時，把 page 改成最後一頁再查一次。
	//     ・每一張卡片顯示的狀態，promote_list.html 已經用時間的判斷式寫好了，這裡不用放。
	//
	//
	// ══ 步驟 2：活動詳細（新增一個方法）　頁面：promote_list.html ══
	//   卡片上的「詳細」會連到這裡：先顯示這筆活動全部的資訊，對話框下方才是「修改」。
	//   @GetMapping("/promote/detail")
	//   參數：@RequestParam Integer promoteNo，以及和步驟 1 一樣的 status、keyword、page（都是 required = false）、Model model
	//   做的事：和步驟 1 一樣查出那一頁並放進 Model（呼叫 putList），再多放那一筆活動：
	//             model.addAttribute("promoteDetail", 用 promoteNo 查出來的 PromoteVO);
	//           查不到那一筆（例如網址被改過）→ return "redirect:/staff/promote";
	//   return "staff/promote/promote_list";
	//   ★ 要先在 PromoteService 加「用編號查一筆」的方法（查不到時回傳 null，寫法可以參考 AnnouncementService）。
	//
	//
	// ══ 步驟 3：打開新增的表單（改下面的 add 方法）　頁面：promote_form.html ══
	//   多一個參數 Model model，放一個空的活動：
	//     model.addAttribute("promoteForm", new PromoteVO());
	//   return "staff/promote/promote_form";
	//
	//
	// ══ 步驟 4：打開修改的表單（新增一個方法）　頁面：promote_form.html ══
	//   列表頁「活動詳細」下方的「修改」會連到這裡。
	//   @GetMapping("/promote/edit")
	//   參數：@RequestParam Integer promoteNo、Model model、RedirectAttributes redirectAttributes
	//   做的事：model.addAttribute("promoteForm", 用 promoteNo 查出來的 PromoteVO);
	//           查不到那一筆 → return "redirect:/staff/promote";
	//   ★ 已經結束的活動不能修改。畫面只是不顯示「修改」的按鈕，網址還是可以自己輸入，所以這裡要擋：
	//       查出來的活動已經結束（promote.getPromoteEnd() != null && LocalDateTime.now().isAfter(promote.getPromoteEnd())）→
	//         redirectAttributes.addFlashAttribute("message", "已結束的活動不能修改");
	//         return "redirect:/staff/promote";
	//   ★ 進行中的活動可以打開表單，但不能改開始日期。這裡不用處理：promote_form.html 會自己判斷並把開始日期鎖住。
	//     放進 promoteForm 的就是查出來的那一筆。真正要擋的地方在儲存（步驟 5）。
	//   return "staff/promote/promote_form";
	//
	//
	// ══ 步驟 5：儲存（新增一個方法）　成功回列表頁，不通過回 promote_form.html ══
	//   用老師範例的寫法：表單直接裝進 PromoteVO，用 @Valid 檢查，錯誤放在 BindingResult。
	//
	//   @PostMapping("/promote/save")，方法要加 throws IOException（讀圖片檔案會用到）
	//   參數（順序有規定：BindingResult 一定要緊接在 @Valid 的那個參數後面）：
	//     @Valid @ModelAttribute("promoteForm") PromoteVO promoteForm
	//           表單的欄位會依照 name 自動放進去：promoteNo、promoteName、promoteBegin、promoteEnd、promoteContent。
	//           ★ @ModelAttribute("promoteForm") 的名稱要和表單頁用的名稱一樣（promoteForm）。
	//             這樣不通過、回傳表單頁的時候，Spring 會自動把這個物件放進 Model，使用者填的內容就會留在表單上，
	//             不用自己再 model.addAttribute 一次。
	//     BindingResult result
	//           裝錯誤的地方：@Valid 檢查出來的，還有「文字轉不成時間」這種轉換失敗的。
	//           有了它，檢查不通過時不會變成 400 錯誤頁，而是進到方法裡讓我們自己處理。
	//     @RequestParam(value = "upImg", required = false) MultipartFile upImg
	//           活動圖片。它的 name 是 upImg，和 VO 的 promoteImg 不同名，所以不會被自動放進 VO，要自己處理。
	//     Model model、RedirectAttributes redirectAttributes
	//     （表單還會送 status、keyword、page：列表頁原本的查詢條件，可以不收，用途見下面「通過」）
	//
	//   ── PromoteVO 要準備的標註（寫在 PromoteVO.java 的欄位上；@Valid 靠它們檢查）──
	//     promoteName      加 @NotBlank（不能是空白），message 寫「請輸入活動名稱」。
	//                      現在的 @Size(min=1, max=10) 建議改成只留 max=10：
	//                      空白交給 @NotBlank，這樣「沒有填」和「太長」會顯示不一樣的訊息。
	//     promoteBegin     加 @NotNull，message 寫「請選擇開始日期」
	//     promoteEnd       加 @NotNull，message 寫「請選擇結束日期」
	//                      （這兩個不能用 @NotBlank：@NotBlank 只能用在文字，時間要用 @NotNull）
	//                      建議再加 @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")，標明表單送來的格式。
	//                      不加也接得到（Spring 會自己認這種標準的格式），加了比較清楚。
	//     promoteContent   現在的 @Size(max=500) 不用改。
	//     ★ 這些標註只在「有 @Valid 的地方」才會檢查，直接存進資料庫時不會，所以不影響其他功能。
	//     實際試過：照上面的標註，名稱空白、日期沒選、名稱 11 個字都會被擋下來，並拿得到 message 的文字。
	//
	//   流程：
	//     1. 先看 @Valid 的結果：
	//          if (result.hasErrors()) {
	//              取第一個錯誤的訊息放進 errorMsg，例如
	//                FieldError error = result.getFieldErrors().get(0);
	//                String text = error.getDefaultMessage();      // 就是標註上寫的 message
	//              ★ 轉換失敗的錯誤（error.getCode() 是 "typeMismatch"，例如日期的文字被改成奇怪的格式）
	//                它的訊息是一長串英文，要換成自己的文字，例如「日期的格式不正確，請重新選擇」。
	//              model.addAttribute("errorMsg", text);
	//              return "staff/promote/promote_form";     ← promoteForm 已經自動在 Model 裡了
	//          }
	//          （一次有好幾個錯誤時，哪一個排第一個不固定；只顯示一個就好。）
	//     2. 修改（promoteForm.getPromoteNo() 不是 null）→ 先查出資料庫原本那一筆。
	//        查不到，或它已經結束 → 帶訊息 redirect 回 /staff/promote，不要存。
	//        ★ 是看「資料庫裡原本的結束時間」，不是表單送來的新時間，不然把日期改晚一點就能繞過去。
	//     3. @Valid 做不到的檢查（建議寫在 PromoteService，回傳錯誤訊息的文字，沒有錯誤回傳 null）：
	//        開始日期不能早於今天（進行中的活動除外）、結束日期不能早於今天、結束日期最早是開始日期的隔天。
	//        完整的規則在 PromoteService.java 的「日期與時間的規則」。
	//        不通過 → model.addAttribute("errorMsg", 訊息); return "staff/promote/promote_form";
	//     4. ★ 把時間設成固定的幾點幾分：開始是那一天的 00:00，結束是那一天的 23:59。
	//        表單送來的時間雖然已經是 00:00 和 23:59，但那是畫面上的 JS 補的，可以被改，所以 Java 要自己再設一次：
	//          開始：promoteForm.getPromoteBegin().toLocalDate().atStartOfDay()
	//          結束：promoteForm.getPromoteEnd().toLocalDate().atTime(23, 59)
	//     5. 圖片：有選檔案（upImg != null && !upImg.isEmpty()）→ 用 upImg.getBytes() 當作圖片
	//              沒有選檔案 → 新增時沒有圖片；修改時保留原本的，不要清掉
	//     6. 存檔：
	//          新增（promoteNo 是 null）→ 可以直接存 promoteForm 這個物件（時間、圖片照 4、5 設好之後）。
	//          修改（promoteNo 有值）  → ★ 不要直接存 promoteForm！它是用表單組出來的，裡面沒有圖片，
	//                                    直接存的話原本的圖片會被清掉，進行中的開始時間也會被改掉。
	//                                    要把欄位「搬」到第 2 點查出來的那一筆，再存那一筆：
	//                                      名稱、內容、結束時間 → 用表單的
	//                                      開始時間 → 進行中的活動保留原本的；未開始的用表單的
	//                                      圖片 → 有上傳新的才換
	//        ★ 3 的檢查要在「搬欄位之前」全部做完。查出來的那一筆只要欄位被改過，之後可能會被自動存進資料庫，
	//          先搬再檢查的話，不通過的資料也可能被存進去。
	//   通過：
	//     redirectAttributes.addFlashAttribute("message", "已新增活動");   ← 修改時換成「已修改活動」
	//     return "redirect:/staff/promote";      ← 回到列表頁的第 1 頁，列表頁會顯示這個訊息
	//     （加分：想回到列表原本那一頁，就多收 status、keyword、page（@RequestParam，required = false），redirect 之前用
	//       redirectAttributes.addAttribute("status", status) 這種寫法放回網址，中文會自動轉好；
	//       不要自己用 + 把中文接在 "redirect:..." 後面。不做也沒關係。）
	//   不通過、回表單頁時要注意：
	//     ・使用者填的名稱、日期、內容會自動留著（因為 promoteForm 自動在 Model 裡）。
	//     ・使用者剛剛選的圖片檔案一定會不見（瀏覽器不能幫忙填回檔案欄位），要請他重選。
	//     ・修改的情況，promoteForm 裡沒有圖片，預覽會顯示「尚無圖片」。想顯示目前的圖片，回傳前把原本那一筆的圖片
	//       設給 promoteForm（promoteForm.setPromoteImg(原本那一筆.getPromoteImg())）。
	//       promoteForm 不是從資料庫查出來的物件，改它不會被自動存檔，可以放心設。
	//
	//
	// ══ 步驟 6：輸出圖片（新增一個方法）══
	//   卡片上的圖片、「活動詳細」和表單頁的預覽，都是用 <img src="/staff/promote/img?promoteNo=1"> 顯示。
	//   這個方法沒寫的話，有圖片的活動會顯示成破圖。
	//     @GetMapping("/promote/img")
	//     public ResponseEntity<byte[]> promoteImg(@RequestParam Integer promoteNo) {
	//         用 promoteNo 查出活動；查不到，或圖片是 null、長度是 0 → return ResponseEntity.notFound().build();
	//         return ResponseEntity.ok()
	//                 .contentType(MediaType.IMAGE_JPEG)
	//                 .header(HttpHeaders.CACHE_CONTROL, "no-cache")   // 不要用瀏覽器暫存的舊圖，換了圖片才會馬上看到新的
	//                 .body(promote.getPromoteImg());
	//     }
	//   ・和公告管理 AnnouncementController 的 image 方法是同樣的寫法，可以對照著看。
	//   ・種類寫 IMAGE_JPEG：使用者上傳 PNG 時瀏覽器的 <img> 一樣顯示得出來，不用另外判斷。
	//   ・回傳的是圖片的內容、不是頁面，所以回傳型別是 ResponseEntity<byte[]>，不要回傳頁面的名稱。
	//   ・圖片存什麼就取什麼，不用轉檔、不用改大小。畫面上固定用 16:9 顯示是 CSS 做的，和這裡無關。
	//
	//   圖片大小的限制：
	//     前端 5MB（promote.js 的 IMG_MAX_MB，選檔案時就擋）；後端 10MB（application.properties 的 max-file-size）。
	//     ・前端的檢查可以被跳過，所以後端的限制要留著。超過後端限制時 Spring 會丟出 MaxUploadSizeExceededException，
	//       使用者看到的是錯誤頁；想顯示友善的訊息，可以另外寫處理這個例外的方法（不寫也不影響正常使用）。
	//     ・max-request-size（整個請求的上限）目前是註解掉的，預設 10MB。要調整的話兩個一起調，而且前端的數字要比後端小。
	//     ・資料表的 MEDIUMBLOB 最多可以存 16MB。
	//
	//   首頁的輪播之後要顯示活動圖片時：
	//     前台要另外寫一個輸出圖片的方法（網址放在 /bistroops 底下，例如 /bistroops/promote/img），
	//     不要讓客人的頁面去連 /staff 的網址（之後員工登入做了攔截，客人就看不到圖片了）。
	//     首頁要放哪些活動、輪播怎麼寫，在 frontend_index.html 輪播上方的說明。
	//
	//
	// ══ 步驟 7：★【最後再做】套用已有活動 ══
	//   請等步驟 1～6、9 都完成、資料庫的畫面確認沒問題之後，再回來做。
	//   在那之前，資料庫那一份的「套用已有活動」按鈕只會跳出「尚未開放」，不影響其他功能。
	//
	//   功能：新增活動時，按「套用已有活動」→ 跳出搜尋的對話框（一開始不列出任何活動）
	//         → 輸入活動名稱按「搜尋」→ 顯示符合的「已經結束」的活動（名稱和結束時間）
	//         → 按「套用」→ 回到新增的表單，活動名稱、活動內容帶進來，再選日期就可以儲存。
	//         存起來的是一筆新的活動，不會改到原本那一個。
	//   ★ 只套用「活動名稱」和「活動內容」，不套用圖片。新的活動需要圖片的話，由使用者自己上傳。
	//     所以只要改 add 方法；save 方法完全不用為了這個功能修改。
	//   畫面的樣子可以先操作假資料的版本。HTML 要怎麼改（按鈕、挑選的對話框）
	//   寫在 promote_form.html【資料庫資料】表單裡，用 ▼▼▼▲▲▲ 框起來的那一段。
	//
	//   add 方法要改的地方（不用 JSON，都是多收幾個參數，都是 required = false）：
	//     (a) 多收三個參數：
	//           String pick          有值代表要顯示挑選的對話框
	//           String pickKeyword   挑選時搜尋的活動名稱
	//           Integer copyFrom     按了「套用」的那個活動編號
	//     (b) 有 pick → Model 一定要放 pickList 和 pickKeyword（挑選的對話框靠 pickList 不是 null 才會出現）：
	//           pickKeyword 是 null 或空白（剛打開、還沒搜尋）→ 不要查資料庫，直接放空的清單：
	//               model.addAttribute("pickList", List.of());
	//             ★ 不要一打開就把已結束的活動全部列出來。
	//           pickKeyword 有內容 → 查出「已經結束」而且名稱裡有 pickKeyword 的活動放進 pickList。
	//             PromoteService 的 getPromoteByEnd 就是做這件事的，它回傳的是 Page，
	//             放進 Model 的要是裡面的清單：getPromoteByEnd(...).getContent()
	//           兩種情況都要 model.addAttribute("pickKeyword", pickKeyword);
	//     (c) 有 copyFrom → 查出那個活動。查不到，或它還沒結束，就當作沒有 copyFrom（放空的 PromoteVO）。
	//         是已結束的活動 → new 一個新的 PromoteVO，只設定活動名稱和活動內容
	//         （不要設定編號、開始、結束時間、圖片），當作 promoteForm。
	//   要注意：
	//     ・copyFrom 是從網址來的，可以被改，所以查出來之後要確認它真的已經結束才套用。
	//     ・去挑選、按套用都會重新載入頁面，使用者之前填的內容會不見，所以畫面上請他「先套用、再選日期」。
	//
	//
	// ══ 步驟 8：★【之後的優化，可以不做】列表的查詢不要把圖片讀出來 ══
	//   現在步驟 1 查出來的是完整的 PromoteVO，每一筆都會把圖片從資料庫讀出來（一頁最多 8 張），
	//   但列表頁只用它判斷「有沒有圖片」，真正顯示時瀏覽器會另外連步驟 6 的網址再讀一次。
	//   活動不多的時候沒關係；想改善的話，把查詢改成回傳只有文字欄位的「小物件」。
	//   完整的做法寫在 PromoteRepository.java 的「之後的優化」。
	//   這個 Controller 幾乎不用改：只有步驟 1 (b) 的 Page<PromoteVO> 要換成 Page<小物件的型別>。
	//
	//
	// ══ 步驟 9：刪除未開始的活動（新增一個方法）　刪完回列表頁 ══
	//   列表頁「活動詳細」下方左邊的「刪除」會送到這裡。只有狀態是「未開始」的活動才看得到那顆按鈕，
	//   按下去會先跳出確認的視窗（promote.js 做的），使用者按確定才會送出。
	//
	//   @PostMapping("/promote/delete")
	//     ★ 一定要用 @PostMapping，不要用 @GetMapping：刪除會改到資料，
	//       用 GET 的話，只要有人打開那個網址（甚至瀏覽器預先載入連結）資料就被刪掉了。
	//   參數：@RequestParam(required = false) Integer promoteNo、RedirectAttributes redirectAttributes
	//         （表單還會送 status、keyword、page：列表頁原本的查詢條件，可以不收。想刪完回到原本那一頁才需要）
	//   流程：
	//     1. promoteNo 是 null，或查不到那一筆 → 帶訊息（例如「查無此活動」）redirect 回 /staff/promote。
	//     2. ★ 確認它真的是「未開始」：promoteSve.getStatus(那一筆) 不是 "未開始" → 帶訊息
	//        「只有未開始的活動可以刪除」redirect 回去，不要刪。
	//        畫面只是把按鈕藏起來，表單還是可以自己送，所以這一步不能省。
	//     3. 呼叫 PromoteService 刪除。
	//     4. redirectAttributes.addFlashAttribute("message", "已刪除活動");
	//        return "redirect:/staff/promote";
	//   ★ 1～3 已經包在 PromoteService 的 deleteNotStartPromote(promoteNo) 裡了：
	//     不能刪的時候它回傳原因的文字，刪除成功回傳 null。所以這個方法只要：
	//       String error = promoteSve.deleteNotStartPromote(promoteNo);
	//       訊息用 error（不是 null 的話），是 null 就用「已刪除活動」，帶回列表頁。
	//     不要呼叫 deletePromoteByNo，那一個沒有任何檢查。
	//   要注意：
	//     ・活動底下的「活動商品」會跟著一起刪（PromoteVO 的 promoteMeals 設定了 cascade = CascadeType.REMOVE）。
	//       「訂單明細」不會刪：已經有訂單用到的活動不能刪除，PromoteService 會回傳原因。
	//       這些都在 PromoteService 的 deleteNotStartPromote 裡處理好了，說明在 PromoteService.java 的功能 8。
	//     ・刪除之後回到的是列表的第 1 頁。那一頁如果剛好被刪到沒有資料也沒關係，步驟 1 會把頁數拉回來。
	//
	//
	// ══ 全部做完之後 ══
	//   步驟 1～6、9 確認資料庫的畫面沒問題後，照 promote_list.html 開頭的步驟刪掉假資料
	//   （步驟 7 建議在刪掉假資料之前做，不然假資料的樣子就看不到了）。
	//   刪掉假資料之後，下面 add 方法上方關於 demoNo 的那一行說明也可以拿掉。

	@Autowired
	PromoteService promoteSve;
	
	@GetMapping("/promote")
	public String promote(@RequestParam(required = false) String status,
						  @RequestParam(required = false) String keyword,
			              @RequestParam(required = false) Integer page,
			              Model model) {
		if (page == null || page < 1) page = 1;
		
		LocalDateTime now = LocalDateTime.now();
		Page<PromoteVO> result=promoteSve.getPromoteByStatus(status, keyword, now, page);
		
		int totalPages = Math.max(1, result.getTotalPages());
		
		if (page > totalPages) {
	        page = totalPages;
	        result = promoteSve.getPromoteByStatus(status, keyword, now, page);
	    }
		
		model.addAttribute("promotes", result.getContent());
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("total", result.getTotalElements());
		model.addAttribute("page", page);
		model.addAttribute("status", status);
		model.addAttribute("keyword", keyword);
		
		return "staff/promote/promote_list";
	}
	
	@GetMapping("/promote/detail")
	public String promoteDetail(@RequestParam(required = false) String status,
								@RequestParam(required = false) String keyword,
								@RequestParam(required = false) Integer page,
								@RequestParam(required = false) Integer promoteNo,
								Model model) {
		if (page == null || page < 1) page = 1;
		
		LocalDateTime now = LocalDateTime.now();
		Page<PromoteVO> result=promoteSve.getPromoteByStatus(status, keyword, now, page);
		
		int totalPages = Math.max(1, result.getTotalPages());
		
		if (page > totalPages) {
	        page = totalPages;
	        result = promoteSve.getPromoteByStatus(status, keyword, now, page);
	    }
		
		model.addAttribute("promotes", result.getContent());
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("total", result.getTotalElements());
		model.addAttribute("page", page);
		model.addAttribute("status", status);
		model.addAttribute("keyword", keyword);
		
		PromoteVO promoteDetail = promoteSve.getPromoteByNo(promoteNo);
		model.addAttribute("promoteDetail", promoteDetail);
		
		if (promoteDetail == null) {
			return "redirect:/staff/promote";
		}		
		return "staff/promote/promote_list";
	}
	
	// 步驟 3：表單頁（新增）。目前只打開頁面，讓假資料的表單看得到。
	// 假資料的「修改」也是連到這個網址（/staff/promote/add?demoNo=編號）：demoNo 是假資料自己用的參數，這裡不用收。
	@GetMapping("/promote/add")
	public String add(Model model) {
		model.addAttribute("promoteForm", new PromoteVO());
		return "staff/promote/promote_form";
	}
	
	@GetMapping("/promote/edit")
	public String edit(@RequestParam (required = false) Integer promoteNo, 
						Model model, RedirectAttributes redirectAttributes) {
		PromoteVO promoteForm = promoteSve.getPromoteByNo(promoteNo);
		
		if (promoteForm == null) {
			redirectAttributes.addFlashAttribute("message", "查無此活動");
			return "redirect:/staff/promote";
		}
		
		if (promoteSve.getStatus(promoteForm).equals("已結束")) {
			redirectAttributes.addFlashAttribute("message", "已結束的活動不能修改");
			return "redirect:/staff/promote";
		}
		
		model.addAttribute("promoteForm", promoteForm);
		return "staff/promote/promote_form";
	}
	
	@PostMapping("/promote/save")
	public String addOrUndate(@Valid @ModelAttribute("promoteForm")PromoteVO promoteVO,BindingResult result,
						 @RequestParam(value = "upImg", required = false) MultipartFile upImg,
						 Model model,
						 RedirectAttributes redirectAttributes) throws IOException {
		if (result.hasErrors()) {
			FieldError error = result.getFieldErrors().get(0);
			String text;
			if("typeMismatch".equals(error.getCode())) {
				text = "日期的格式不正確，請重新選擇";
			} else {
				text = error.getDefaultMessage();
			}
			
			model.addAttribute("errorMsg", text);
			return "staff/promote/promote_form";
		}
		
		// 2. 查出資料庫原本那一筆
		PromoteVO originalPromote = null;
		if (promoteVO.getPromoteNo() != null) {
			originalPromote = promoteSve.getPromoteByNo(promoteVO.getPromoteNo());
			if (originalPromote == null || promoteSve.getStatus(originalPromote).equals("已結束")) {
				redirectAttributes.addFlashAttribute("message", "查無此活動或已結束的活動不能修改");
				return "redirect:/staff/promote";
			}
		}
		
		// 3. 日期檢查
		String dateErrorMsg;
		if (originalPromote == null) {
		    dateErrorMsg = promoteSve.checkNotStartPromoteDate(promoteVO);
		} else if(promoteSve.getStatus(originalPromote).equals("未開始")) {
			dateErrorMsg = promoteSve.checkNotStartPromoteDate(promoteVO);
		} else { 
			dateErrorMsg = promoteSve.checkActivePromoteDate(promoteVO, originalPromote);
		}
		
		if (dateErrorMsg != null) {
		    if (originalPromote != null) {
		        promoteVO.setPromoteImg(originalPromote.getPromoteImg());
		    }
		    model.addAttribute("errorMsg", dateErrorMsg);
		    return "staff/promote/promote_form";
		}
		
		//4. 圖片
		if(upImg != null && !upImg.isEmpty()) {
			promoteVO.setPromoteImg(upImg.getBytes());
		}
		
		// 5. 存檔
		if (originalPromote == null) {
			promoteSve.addPromote(promoteVO,promoteVO.getPromoteImg());
			redirectAttributes.addFlashAttribute("message", "已新增活動");
		}
		else {
			PromoteVO update = promoteSve.updatePromote(
					promoteVO.getPromoteNo(), promoteVO.getPromoteName(), promoteVO.getPromoteContent(),
					promoteVO.getPromoteBegin(), promoteVO.getPromoteEnd(), promoteVO.getPromoteImg());
			if ( update == null ) {
				redirectAttributes.addFlashAttribute("message", "修改活動失敗請重新輸入");				
			} else {
			redirectAttributes.addFlashAttribute("message", "已修改活動");
			}
		
		}
		return "redirect:/staff/promote";
	}
	
	
	@GetMapping("/promote/img")
	public ResponseEntity<byte[]> promoteImg(@RequestParam (required = false) Integer promoteNo) {
		PromoteVO promoteVO = promoteSve.getPromoteByNo(promoteNo);
		
		byte[] img = (promoteVO != null) ? promoteVO.getPromoteImg() : null;
		
		if( img == null || img.length == 0) {
			return ResponseEntity.notFound().build();
		}
		
		return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")   
                .body(img);
	}

	
}
