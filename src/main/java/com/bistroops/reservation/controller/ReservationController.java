package com.bistroops.reservation.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bistroops.reservation.model.ReservationService;
import com.bistroops.reservation.model.ReservationVO;

@Controller
@RequestMapping("/staff/reservations")
public class ReservationController {

	private static final Set<String> STATUSES = Set.of("預約", "實到", "未到", "取消");

	private static final Set<String> PERIODS = Set.of("all", "lunch", "dinner");

	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {

		this.reservationService = reservationService;
	}

	@GetMapping
	public String list(
			@RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

			@RequestParam(name = "status", defaultValue = "") String status,

			@RequestParam(name = "period", defaultValue = "all") String period,

			@RequestParam(name = "keyword", defaultValue = "") String keyword,

			@RequestParam(name = "selectedRsvNo", required = false) Long selectedRsvNo,

			Model model) {

		// 預設查詢今日；其他日期由日期欄位選擇。
		LocalDate selectedDate = date == null ? LocalDate.now() : date;

		String selectedStatus = normalizeStatus(status);
		String selectedPeriod = normalizePeriod(period);
		String selectedKeyword = normalizeKeyword(keyword);

		List<ReservationVO> reservations = reservationService.search(selectedDate, selectedStatus);

		List<ReservationVO> filtered = reservations.stream().filter(r -> matchesPeriod(r, selectedPeriod))
				.filter(r -> matchesKeyword(r, selectedKeyword)).toList();

		// 依時段分組，保留查詢結果原本的時間排序。
		List<LocalTime> timeGroups = filtered.stream().map(r -> r.getRsvDt().getRsvDtDatetime().toLocalTime())
				.distinct().sorted().toList();

		ReservationVO selected = null;

		if (selectedRsvNo != null) {
			selected = filtered.stream().filter(r -> selectedRsvNo.equals(r.getRsvNo())).findFirst().orElse(null);
		}

		model.addAttribute("rsvList", filtered);
		model.addAttribute("timeGroups", timeGroups);
		model.addAttribute("selectedReservation", selected);

		model.addAttribute("selectedDate", selectedDate.toString());

		model.addAttribute("selectedStatus", selectedStatus);

		model.addAttribute("selectedPeriod", selectedPeriod);

		model.addAttribute("selectedKeyword", selectedKeyword);

		model.addAttribute("resultCount", filtered.size());

		return "staff/reservation/list";
	}

	// 同一個入口接收左側管理區的操作。
	@PostMapping("/action")
	public String action(@RequestParam(name = "rsvNo") Long rsvNo,

			@RequestParam(name = "operation") String operation,

			@RequestParam(name = "rsvComment", defaultValue = "") String rsvComment,

			@RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

			@RequestParam(name = "status", defaultValue = "") String status,

			@RequestParam(name = "period", defaultValue = "all") String period,

			@RequestParam(name = "keyword", defaultValue = "") String keyword,

			RedirectAttributes redirectAttributes) {

		try {
			String message;

			switch (operation) {
			case "comment" -> {
				reservationService.updateComment(rsvNo, rsvComment);

				message = "訂位備註已更新";
			}

			case "cancel" -> {
				reservationService.cancelByStaff(rsvNo);

				message = "訂位已取消";
			}

			case "arrived" -> {
				reservationService.markArrivedByStaff(rsvNo);

				message = "訂位已標記為實到";
			}

			case "no-show" -> {
				reservationService.markNoShowByStaff(rsvNo);

				message = "訂位已標記為未到";
			}

			default -> throw new IllegalArgumentException("不支援的訂位操作");
			}

			redirectAttributes.addFlashAttribute("message", message);

		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		}

		// 操作後保留查詢條件與選取的編號。
		redirectAttributes.addAttribute("date", date.toString());

		redirectAttributes.addAttribute("status", normalizeStatus(status));

		redirectAttributes.addAttribute("period", normalizePeriod(period));

		redirectAttributes.addAttribute("keyword", normalizeKeyword(keyword));

		redirectAttributes.addAttribute("selectedRsvNo", rsvNo);

		return "redirect:/staff/reservations";
	}

	private boolean matchesPeriod(ReservationVO reservation, String period) {

		LocalTime time = reservation.getRsvDt().getRsvDtDatetime().toLocalTime();

		return switch (period) {
		case "lunch" -> time.isBefore(LocalTime.of(15, 0));
		case "dinner" -> !time.isBefore(LocalTime.of(15, 0));
		default -> true;
		};
	}

	private boolean matchesKeyword(ReservationVO reservation, String keyword) {

		if (keyword.isEmpty()) {
			return true;
		}

		String searchText = (reservation.getRsvNo() + " " + safe(reservation.getMember().getMemName()) + " "
				+ safe(reservation.getMember().getMemTel()) + " " + safe(reservation.getRsvComment()))
				.toLowerCase(Locale.ROOT);

		return searchText.contains(keyword.toLowerCase(Locale.ROOT));
	}

	private String normalizeStatus(String status) {

		String value = safe(status).trim();

		return STATUSES.contains(value) ? value : "";
	}

	private String normalizePeriod(String period) {

		String value = safe(period).trim();

		return PERIODS.contains(value) ? value : "all";
	}

	private String normalizeKeyword(String keyword) {

		String value = safe(keyword).trim();

		// 搜尋欄限制長度，直接送出超長內容時也處理。
		return value.length() > 100 ? value.substring(0, 100) : value;
	}

	private String safe(String value) {
		return value == null ? "" : value;
	}
}