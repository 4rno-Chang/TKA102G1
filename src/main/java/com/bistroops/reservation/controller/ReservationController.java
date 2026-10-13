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
import com.bistroops.reservationdatetime.model.ReservationDatetimeService;

@Controller
@RequestMapping("/staff/reservations")
public class ReservationController {

	private static final Set<String> STATUSES = Set.of("預約", "實到", "未到", "取消");

	private static final Set<String> PERIODS = Set.of("all", "lunch", "dinner");

	private final ReservationService reservationService;
	private final ReservationDatetimeService reservationDatetimeService;

	public ReservationController(ReservationService reservationService,
			ReservationDatetimeService reservationDatetimeService) {

		this.reservationService = reservationService;
		this.reservationDatetimeService = reservationDatetimeService;
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

	// 開啟左側新增表單，也可重新查詢日期與桌型的時段。
	@GetMapping("/new")
	public String showCreate(
			@RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

			@RequestParam(name = "tableType", defaultValue = "2") Integer tableType,

			@RequestParam(name = "phone", defaultValue = "") String phone,

			@RequestParam(name = "note", defaultValue = "") String note,

			Model model) {

		LocalDate selectedDate = date == null ? LocalDate.now() : date;

		Integer selectedTableType = Set.of(2, 4, 6).contains(tableType) ? tableType : 2;

		// 使用原本的清單方法，右側顯示同一天的訂位。
		list(selectedDate, "", "all", "", null, model);

		model.addAttribute("staffCreateMode", true);

		model.addAttribute("createDate", selectedDate.toString());

		model.addAttribute("createTableType", selectedTableType);

		model.addAttribute("createMinDate", LocalDate.now().toString());

		model.addAttribute("createMaxDate", LocalDate.now(java.time.ZoneId.of("Asia/Taipei")).plusDays(89).toString());

		model.addAttribute("createPhone", phone);

		model.addAttribute("createNote", note);

		model.addAttribute("createSlots", reservationService.getSlots(selectedDate, selectedTableType));

		model.addAttribute("createOpenDates", reservationDatetimeService.getOpenDates());

		return "staff/reservation/list";
	}

	// 送出後台新增訂位。
	@PostMapping("/create")
	public String create(@RequestParam(name = "phone", defaultValue = "") String phone,

			@RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

			@RequestParam(name = "tableType") Integer tableType,

			@RequestParam(name = "rsvDtNo", required = false) Long rsvDtNo,

			@RequestParam(name = "note", defaultValue = "") String note,

			RedirectAttributes redirectAttributes) {

		try {
			// 防止直接送出未開放日期。
			if (!reservationDatetimeService.getOpenDates().contains(date)) {

				throw new IllegalArgumentException("這一天沒有開放可預約時段");
			}

			ReservationVO reservation = reservationService.addByStaff(phone, date, rsvDtNo, tableType, note);

			redirectAttributes.addFlashAttribute("message", "新增訂位成功");

			redirectAttributes.addAttribute("date", date.toString());

			redirectAttributes.addAttribute("selectedRsvNo", reservation.getRsvNo());

			return "redirect:/staff/reservations";

		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());

			// 新增失敗時保留輸入內容，不放在網址中。
			redirectAttributes.addFlashAttribute("staffCreatePhone", phone);

			redirectAttributes.addFlashAttribute("staffCreateNote", note);

			redirectAttributes.addAttribute("date", date.toString());

			redirectAttributes.addAttribute("tableType", tableType);

			return "redirect:/staff/reservations/new";
		}
	}
}