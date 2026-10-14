package com.bistroops.seattype.controller;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bistroops.seattype.model.SeatTypeService;
import com.bistroops.waiting.model.WaitingUnavailableException;

/*
 * 入座管理 /staff/seating
 * 這裡不檢查權限；有後台登入的專案（例如 05 的 StaffSecurityConfig）可以把 /staff/seating/** 設成 PERM_3（入座管理）
 *
 * GET  /staff/seating?view=floor|timeline&type=all|2|4|6&state=all|free|use|off|late|anomaly&seat=A02
 *      type（桌型）和 state（狀態，平面圖的說明列）可以同時篩選
 *      focus（只有時間軸用）＝all|busy|rsv|warn：點上方卡片後只顯示用餐中／90 分鐘內的預約／須注意
 *      先呼叫 SeatTypeService.releasePaidSeats()：已結帳的桌子清回空桌（結帳＝空桌，不用按客人離席）
 *      再由 SeatTypeService.getPage() 算好整頁的資料放進 Model 的 page（一個 Map），staff/seating.html 用 ${page.xxx} 顯示：
 *        view、filter、state、selected  目前的畫面、桌型、狀態、選到的桌號（就是網址參數）
 *        stateOptions            平面圖說明列（狀態篩選）的選項
 *        now、est                現在時間（HH:mm）、固定用餐分鐘數（90）
 *        seats                   平面圖上的每一張桌子
 *        panel                   選到的那一桌（沒選是 null）：狀態、結帳、客人資訊、可選的訂位／候位
 *        closing                 打烊清桌的確認文字
 *        timeline                時間軸（view=timeline 才有，否則是 null）
 * POST /staff/seating/{walk-in, reservation, waiting, leave, disable, enable, closing}
 *      做完都導回 GET（Post/Redirect/Get，重新整理不會重送），結果放在 flash 的 message／error
 */
@Controller
@RequestMapping("/staff/seating")
public class SeatTypeController {

	private static final Logger log = LoggerFactory.getLogger(SeatTypeController.class);

	private final SeatTypeService seatTypeService;

	public SeatTypeController(SeatTypeService seatTypeService) {
		this.seatTypeService = seatTypeService;
	}

	@GetMapping
	public String page(@RequestParam(defaultValue = "floor") String view,
			@RequestParam(defaultValue = "all") String type, @RequestParam(defaultValue = "all") String state,
			@RequestParam(defaultValue = "all") String focus, @RequestParam(required = false) String seat, Model model) {
		seatTypeService.releasePaidSeats();   // 已結帳的桌子先清回空桌，再算畫面
		model.addAttribute("page", seatTypeService.getPage(view, type, state, focus, seat));
		return "staff/seating";
	}

	@PostMapping("/walk-in")
	public String walkIn(@RequestParam String seatNo, @RequestParam(defaultValue = "all") String type,
			@RequestParam(defaultValue = "floor") String view,
			@RequestParam(defaultValue = "all") String state,
			@RequestParam(defaultValue = "all") String focus, RedirectAttributes ra) {
		return act(ra, seatNo, type, state, focus, view, () -> {
			seatTypeService.seatWalkIn(seatNo);
			return seatNo + "：其它者入座";
		});
	}

	@PostMapping("/reservation")
	public String reservation(@RequestParam String seatNo, @RequestParam(required = false) Long rsvNo,
			@RequestParam(defaultValue = "all") String type, @RequestParam(defaultValue = "floor") String view,
			@RequestParam(defaultValue = "all") String state,
			@RequestParam(defaultValue = "all") String focus, RedirectAttributes ra) {
		return act(ra, seatNo, type, state, focus, view, () -> {
			seatTypeService.seatReservation(seatNo, rsvNo);
			return seatNo + "：訂位者入座，訂位 #" + rsvNo + " 改成「實到」";
		});
	}

	@PostMapping("/waiting")
	public String waiting(@RequestParam String seatNo, @RequestParam(required = false) String ticketId,
			@RequestParam(defaultValue = "") String ticketNumber,
			@RequestParam(defaultValue = "all") String type, @RequestParam(defaultValue = "floor") String view,
			@RequestParam(defaultValue = "all") String state,
			@RequestParam(defaultValue = "all") String focus, RedirectAttributes ra) {
		return act(ra, seatNo, type, state, focus, view, () -> {
			seatTypeService.seatWaiting(seatNo, ticketId);
			return seatNo + "：候位入座，號碼 " + ticketNumber + " 改成「已入座」";
		});
	}

	@PostMapping("/leave")
	public String leave(@RequestParam String seatNo, @RequestParam(defaultValue = "all") String type,
			@RequestParam(defaultValue = "floor") String view,
			@RequestParam(defaultValue = "all") String state,
			@RequestParam(defaultValue = "all") String focus, RedirectAttributes ra) {
		// 離席後不帶 seat：回到原本的畫面時桌位卡視窗是關著的（客人走了，不需要再看這一桌）
		return act(ra, null, type, state, focus, view, () -> {
			seatTypeService.leave(seatNo);
			return seatNo + "：客人離席，桌子變回可使用";
		});
	}

	@PostMapping("/disable")
	public String disable(@RequestParam String seatNo, @RequestParam(defaultValue = "all") String type,
			@RequestParam(defaultValue = "floor") String view,
			@RequestParam(defaultValue = "all") String state,
			@RequestParam(defaultValue = "all") String focus, RedirectAttributes ra) {
		return act(ra, seatNo, type, state, focus, view, () -> {
			seatTypeService.disable(seatNo);
			return seatNo + "：已停用";
		});
	}

	@PostMapping("/enable")
	public String enable(@RequestParam String seatNo, @RequestParam(defaultValue = "all") String type,
			@RequestParam(defaultValue = "floor") String view,
			@RequestParam(defaultValue = "all") String state,
			@RequestParam(defaultValue = "all") String focus, RedirectAttributes ra) {
		return act(ra, seatNo, type, state, focus, view, () -> {
			seatTypeService.enable(seatNo);
			return seatNo + "：重新啟用";
		});
	}

	@PostMapping("/closing")
	public String closing(@RequestParam(defaultValue = "all") String type,
			@RequestParam(defaultValue = "floor") String view,
			@RequestParam(defaultValue = "all") String state,
			@RequestParam(defaultValue = "all") String focus, RedirectAttributes ra) {
		return act(ra, null, type, state, focus, view, () -> {
			int[] r = seatTypeService.closing();
			return "打烊清桌：清回 " + r[0] + " 桌" + (r[1] > 0 ? "，跳過 " + r[1] + " 桌（還有未結帳訂單）" : "");
		});
	}

	// 執行操作，成功放 message、失敗放 error，導回原本的畫面（view：平面圖或時間軸；保留選到的桌子、桌型和狀態篩選）
	// 時間軸上操作完會回到 ?view=timeline&seat=桌號，桌位卡視窗維持開著
	private String act(RedirectAttributes ra, String seatNo, String type, String state, String focus, String view,
			Supplier<String> action) {
		try {
			ra.addFlashAttribute("message", action.get());
		} catch (IllegalArgumentException | IllegalStateException | WaitingUnavailableException e) {
			// WaitingUnavailableException：Redis 候位連不上，顯示 CJ 寫好的中文訊息
			ra.addFlashAttribute("error", e.getMessage());
		} catch (RuntimeException e) {
			log.error("入座管理操作失敗", e);
			ra.addFlashAttribute("error", "操作失敗，請重新整理後再試一次（" + e.getClass().getSimpleName() + "）");
		}
		ra.addAttribute("view", "timeline".equals(view) ? "timeline" : "floor");
		ra.addAttribute("type", type);
		if ("timeline".equals(view)) {
			ra.addAttribute("focus", focus);   // 時間軸上點卡片的篩選
		} else {
			ra.addAttribute("state", state);   // 狀態篩選只有平面圖用
		}
		if (seatNo != null) {
			ra.addAttribute("seat", seatNo);
		}
		return "redirect:/staff/seating";
	}
}
