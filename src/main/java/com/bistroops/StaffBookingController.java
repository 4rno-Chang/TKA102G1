package com.bistroops;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;

import com.bistroops.sms.SmsSendException;
import com.bistroops.reservationdatetime.config.ReservationReminderScheduler;
import com.bistroops.seattype.model.SeatTypeService;
import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bistroops.employee.model.EmployeeService;
import com.bistroops.reservation.model.ReservationService;
import com.bistroops.reservation.model.ReservationVO;
import com.bistroops.waiting.model.WaitingRedisService;
import com.bistroops.waiting.model.WaitingTicket;
import com.bistroops.waiting.model.WaitingUnavailableException;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/staff")
public class StaffBookingController {

	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Taipei");

	private static final List<Integer> TABLE_TYPES = List.of(2, 4, 6);

	private final ReservationService reservationService;
	private final WaitingRedisService waitingRedisService;
	private final EmployeeService employeeService;
	private final SeatTypeService seatTypeService;
	@Autowired
	private ReservationReminderScheduler reservationReminderScheduler;

	public StaffBookingController(ReservationService reservationService, WaitingRedisService waitingRedisService,
			EmployeeService employeeService, SeatTypeService seatTypeService) {

		this.reservationService = reservationService;
		this.waitingRedisService = waitingRedisService;
		this.employeeService = employeeService;
		this.seatTypeService = seatTypeService;
	}

	@GetMapping("/booking")
	public String booking(@RequestParam(name = "tab", defaultValue = "rsv") String tab,

			@RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

			@RequestParam(name = "meal", defaultValue = "") String meal,

			@RequestParam(name = "keyword", defaultValue = "") String keyword,

			@RequestParam(name = "seatType", required = false) Integer seatType,

			HttpSession session, Model model) {

		Object stored = session.getAttribute("employeeNo");

		if (!(stored instanceof Integer employeeNo)) {
			return "redirect:/staff/login";
		}

		var employee = employeeService.getEmployeeById(employeeNo).orElse(null);

		if (employee == null || !"1".equals(employee.getEmpStatus())) {
			session.removeAttribute("employeeNo");
			return "redirect:/staff/login";
		}

		LocalDate selectedDate = date == null ? LocalDate.now(BUSINESS_ZONE) : date;

		String selectedMeal = normalizeMeal(meal);
		String selectedKeyword = keyword.trim();

		if (selectedKeyword.length() > 100) {
			selectedKeyword = selectedKeyword.substring(0, 100);
		}

		Integer selectedSeatType = seatType != null && TABLE_TYPES.contains(seatType) ? seatType : null;

		boolean waitingTab = "waiting".equals(tab);

		model.addAttribute("tab", waitingTab ? "waiting" : "rsv");
		model.addAttribute("date", selectedDate);
		model.addAttribute("meal", selectedMeal);
		model.addAttribute("keyword", selectedKeyword);
		model.addAttribute("seatType", selectedSeatType);

		model.addAttribute("bookingToken", bookingActionToken(session));
		
		model.addAttribute(
		        "smsConsoleMode",
		        reservationReminderScheduler.isConsoleMode());

		model.addAttribute("bookingNow", java.time.LocalDateTime.now(BUSINESS_ZONE));

		if (waitingTab) {
			loadWaiting(selectedSeatType, model);

		} else {
			loadReservations(selectedDate, selectedMeal, selectedKeyword, model);
		}

		return "staff/reservation/booking";
	}

	private void loadReservations(LocalDate date, String meal, String keyword, Model model) {

		List<ReservationVO> reservations = reservationService.search(date, null).stream()

				.filter(reservation -> !"取消".equals(reservation.getRsvStatus()))

				.filter(reservation -> {
					LocalTime time = reservation.getRsvDt().getRsvDtDatetime().toLocalTime();

					boolean lunch = time.isBefore(LocalTime.of(15, 0));

					return "lunch".equals(meal) ? lunch : !lunch;
				})

				.filter(reservation -> {
					if (keyword.isEmpty()) {
						return true;
					}

					String reservationNo = String.valueOf(reservation.getRsvNo());

					String phone = reservation.getMember().getMemTel();

					return reservationNo.equals(keyword) || (phone != null && phone.contains(keyword));
				})

				.toList();

		model.addAttribute("rsvList", reservations);
	}

	private void loadWaiting(Integer selectedSeatType, Model model) {

		model.addAttribute("waitingAvailable", false);
		model.addAttribute("waitingList", List.of());
		model.addAttribute("seatBoards", List.of());

		model.addAttribute("waitingBusinessDate", LocalDate.now(BUSINESS_ZONE).toString());

		try {
			Map<Integer, Integer> availableTables = seatTypeService.getWaitingAvailableTables();

			List<Map<String, Object>> rows = new ArrayList<>();
			List<Map<String, Object>> boards = new ArrayList<>();

			for (Integer tableType : TABLE_TYPES) {
				List<WaitingTicket> tickets = waitingRedisService.getTodayTickets(tableType);

				List<Map<String, Object>> callingList = tickets.stream()
						.filter(ticket -> "NOTIFIED".equals(ticket.status())).map(this::waitingRow).toList();

				String nextTicketId = tickets.stream().filter(ticket -> "WAITING".equals(ticket.status()))
						.min(java.util.Comparator.comparingLong(WaitingTicket::sequence)).map(WaitingTicket::ticketId)
						.orElse("");

				Map<String, Object> board = new LinkedHashMap<>();

				board.put("seatTypeNo", tableType);
				board.put("pending", availableTables.getOrDefault(tableType, 0));
				board.put("nextTicketId", nextTicketId);
				board.put("callingList", callingList);

				board.put("waitingCount", tickets.stream().filter(waitingRedisService::isActive).count());

				boards.add(board);

				if (selectedSeatType == null || selectedSeatType.equals(tableType)) {

					tickets.stream().filter(ticket -> !"CANCELLED".equals(ticket.status())).map(this::waitingRow)
							.forEach(rows::add);
				}
			}

			model.addAttribute("waitingList", rows);
			model.addAttribute("seatBoards", boards);
			model.addAttribute("waitingAvailable", true);

		} catch (WaitingUnavailableException ex) {
			model.addAttribute("error", ex.getMessage());
		}

	}

	// 欄位名稱配合目前 booking.html，資料來源仍是 Redis。
	private Map<String, Object> waitingRow(WaitingTicket ticket) {

		Map<String, Object> row = new LinkedHashMap<>();

		row.put("waitingNo", ticket.ticketId());
		row.put("waitingTicketSeq", ticket.sequence());
		row.put("ticketDisplay", ticket.getTicketDisplay());

		row.put("seatType", Map.of("seatTypeNo", ticket.tableType()));

		row.put("waitingName", ticket.name() == null || ticket.name().isBlank() ? "未填寫" : ticket.name());

		row.put("waitingTel", ticket.phone());
		row.put("waitingComment", ticket.note());
		row.put("waitingStatus", ticket.getStatusText());

		row.put("waitingNotifyTime", ticket.notifiedAt() <= 0 ? null
				: Instant.ofEpochMilli(ticket.notifiedAt()).atZone(BUSINESS_ZONE).toLocalTime());

		return row;
	}

	private String normalizeMeal(String meal) {

		if ("lunch".equals(meal) || "dinner".equals(meal)) {
			return meal;
		}

		return LocalTime.now(BUSINESS_ZONE).isBefore(LocalTime.of(15, 0)) ? "lunch" : "dinner";
	}

	private void requireBookingEmployee(HttpSession session) {
		Object stored = session.getAttribute("employeeNo");

		if (!(stored instanceof Integer employeeNo)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入員工帳號");
		}

		var employee = employeeService.getEmployeeById(employeeNo).orElse(null);

		if (employee == null || !"1".equals(employee.getEmpStatus())) {
			session.removeAttribute("employeeNo");

			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "員工登入已失效");
		}
	}

	private String bookingActionToken(HttpSession session) {
		synchronized (session) {
			Object employeeNo = session.getAttribute("employeeNo");
			Object owner = session.getAttribute("staffBookingTokenOwner");
			Object stored = session.getAttribute("staffBookingToken");

			if (!java.util.Objects.equals(employeeNo, owner) || !(stored instanceof String)) {

				String token = UUID.randomUUID().toString();

				session.setAttribute("staffBookingTokenOwner", employeeNo);
				session.setAttribute("staffBookingToken", token);

				return token;
			}

			return (String) stored;
		}
	}

	private void checkBookingActionToken(HttpSession session, String submittedToken) {

		synchronized (session) {
			Object employeeNo = session.getAttribute("employeeNo");
			Object owner = session.getAttribute("staffBookingTokenOwner");
			Object expected = session.getAttribute("staffBookingToken");

			if (submittedToken == null || !submittedToken.equals(expected)
					|| !java.util.Objects.equals(employeeNo, owner)) {

				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "表單已失效，請重新整理頁面後操作");
			}
		}
	}

	private Map<String, String> reservationEditorValues(Long rsvNo, LocalDate date, Integer tableType, Long rsvDtNo,
			String phone, String name, String note) {

		Map<String, String> values = new LinkedHashMap<>();

		values.put("rsvNo", rsvNo == null ? "" : rsvNo.toString());
		values.put("date", date.toString());
		values.put("tableType", tableType.toString());
		values.put("rsvDtNo", rsvDtNo == null ? "" : rsvDtNo.toString());
		values.put("phone", phone == null ? "" : phone);
		values.put("name", name == null ? "" : name);
		values.put("note", note == null ? "" : note);

		return values;
	}

	private void prepareReservationEditor(Model model, Map<String, String> values, Long excludedRsvNo) {

		model.addAttribute("staffRsvForm", values);

		model.addAttribute("staffRsvAvailability", reservationService.getAvailabilitySnapshot(excludedRsvNo));
	}

	@GetMapping("/booking/rsv/form")
	public String showReservationEditor(@RequestParam(name = "rsvNo", required = false) Long rsvNo,
			@RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			HttpSession session, Model model) {

		requireBookingEmployee(session);

		LocalDate selectedDate = date == null ? LocalDate.now(BUSINESS_ZONE) : date;

		Integer tableType = 2;
		Long slotNo = null;
		String phone = "";
		String name = "";
		String note = "";
		String meal = "";

		if (rsvNo != null) {
			ReservationVO reservation = reservationService.getOneReservation(rsvNo);

			var diningTime = reservation.getRsvDt().getRsvDtDatetime();

			if (!"預約".equals(reservation.getRsvStatus())
					|| !diningTime.isAfter(java.time.LocalDateTime.now(BUSINESS_ZONE))) {

				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "這筆訂位已無法編輯，請重新整理列表");
			}

			selectedDate = diningTime.toLocalDate();
			meal = diningTime.toLocalTime().isBefore(LocalTime.of(15, 0)) ? "lunch" : "dinner";

			tableType = reservation.getSeatType().getSeatTypeNo();
			slotNo = reservation.getRsvDt().getRsvDtNo();
			phone = reservation.getMember().getMemTel();
			name = reservation.getRsvName();
			note = reservation.getRsvComment();
		}

		booking("rsv", selectedDate, meal, "", null, session, model);

		prepareReservationEditor(model,
				reservationEditorValues(rsvNo, selectedDate, tableType, slotNo, phone, name, note), rsvNo);

		return "staff/reservation/booking";
	}

	@PostMapping("/booking/rsv/save")
	public String saveStaffReservation(@RequestParam(name = "rsvNo", required = false) Long rsvNo,
			@RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(name = "tableType") Integer tableType,
			@RequestParam(name = "rsvDtNo", required = false) Long rsvDtNo,
			@RequestParam(name = "phone", defaultValue = "") String phone,
			@RequestParam(name = "name", defaultValue = "") String name,
			@RequestParam(name = "note", defaultValue = "") String note,
			@RequestParam(name = "bookingToken") String token, HttpSession session, Model model,
			RedirectAttributes redirect) {

		requireBookingEmployee(session);
		checkBookingActionToken(session, token);

		try {
			ReservationVO reservation;

			if (rsvNo == null) {
				reservation = reservationService.addByPhone(phone, name, date, rsvDtNo, tableType, note);
			} else {
				reservationService.editByStaff(rsvNo, date, rsvDtNo, tableType, name, note);

				reservation = reservationService.getOneReservation(rsvNo);
			}

			var diningTime = reservation.getRsvDt().getRsvDtDatetime();

			redirect.addAttribute("date", diningTime.toLocalDate());
			redirect.addAttribute("meal", diningTime.toLocalTime().isBefore(LocalTime.of(15, 0)) ? "lunch" : "dinner");

			redirect.addFlashAttribute("message", (rsvNo == null ? "新增" : "修改") + "訂位成功，編號：" + reservation.getRsvNo());

			return "redirect:/staff/booking";

		} catch (IllegalArgumentException ex) {
			booking("rsv", date, "", "", null, session, model);

			model.addAttribute("staffRsvError", ex.getMessage());

			prepareReservationEditor(model, reservationEditorValues(rsvNo, date, tableType, rsvDtNo, phone, name, note),
					rsvNo);

			return "staff/reservation/booking";
		}
	}

	@PostMapping("/booking/rsv/cancel")
	public String cancelStaffReservation(@RequestParam(name = "rsvNo") Long rsvNo,
			@RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(name = "meal", defaultValue = "") String meal,
			@RequestParam(name = "keyword", defaultValue = "") String keyword,
			@RequestParam(name = "bookingToken") String token, HttpSession session, RedirectAttributes redirect) {

		requireBookingEmployee(session);
		checkBookingActionToken(session, token);

		try {
			reservationService.cancelByStaff(rsvNo);
			redirect.addFlashAttribute("message", "訂位已取消");

		} catch (IllegalArgumentException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}

		redirect.addAttribute("date", date);
		redirect.addAttribute("meal", normalizeMeal(meal));
		redirect.addAttribute("keyword", keyword);

		return "redirect:/staff/booking";
	}

	private void checkWaitingBusinessDate(String submittedDate) {
		String today = LocalDate.now(BUSINESS_ZONE).toString();

		if (!today.equals(submittedDate)) {
			throw new IllegalArgumentException("候位日期已切換，請重新整理頁面後操作");
		}
	}

	@PostMapping("/booking/waiting/join")
	public String createStaffWaiting(@RequestParam(name = "tableType") Integer tableType,
			@RequestParam(name = "phone", defaultValue = "") String phone,
			@RequestParam(name = "name", defaultValue = "") String name,
			@RequestParam(name = "note", defaultValue = "") String note,
			@RequestParam(name = "businessDate") String businessDate, @RequestParam(name = "bookingToken") String token,
			HttpSession session, RedirectAttributes redirect) {

		requireBookingEmployee(session);
		checkBookingActionToken(session, token);

		try {
			checkWaitingBusinessDate(businessDate);

			WaitingTicket ticket = waitingRedisService.takeTicket(tableType, phone, name, note);

			redirect.addFlashAttribute("message", "候位取號完成：" + ticket.getTicketDisplay());

		} catch (IllegalArgumentException | WaitingUnavailableException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());

			redirect.addFlashAttribute("waitingCreate",
					Map.of("tableType", tableType, "phone", phone, "name", name, "note", note));
		}

		redirect.addAttribute("tab", "waiting");

		return "redirect:/staff/booking";
	}

	@PostMapping("/booking/waiting/action")
	public String staffWaitingAction(@RequestParam(name = "operation") String operation,
			@RequestParam(name = "sendSms", required = false) Boolean sendSms,
			@RequestParam(name = "tableType") Integer tableType,
			@RequestParam(name = "ticketId", defaultValue = "") String ticketId,
			@RequestParam(name = "note", defaultValue = "") String note,
			@RequestParam(name = "businessDate") String businessDate, @RequestParam(name = "bookingToken") String token,
			@RequestParam(name = "seatType", required = false) Integer seatType, HttpSession session,
			RedirectAttributes redirect) {

		requireBookingEmployee(session);
		checkBookingActionToken(session, token);

		try {
			checkWaitingBusinessDate(businessDate);

			String message;

			switch (operation) {
			case "NOTIFY" -> {
			    if (sendSms == null) {
			        throw new IllegalArgumentException(
			                "請先選擇是否發送簡訊");
			    }

			    WaitingTicket ticket = waitingRedisService.notifyWaiting(
			            tableType, ticketId, sendSms);

			    message = "已叫號：" + ticket.getTicketDisplay()
			            + (sendSms
			                    ? "；簡訊已處理，開始一分鐘等待"
			                    : "；已人工叫號，開始一分鐘等待");
			}

			case "MISS" -> {
				waitingRedisService.missWaiting(tableType, ticketId);
				message = "候位已設為過號";
			}

			case "CANCEL" -> {
				waitingRedisService.cancelByStaff(tableType, ticketId);
				message = "候位已取消";
			}

			case "COMMENT" -> {
				waitingRedisService.updateCommentByStaff(tableType, ticketId, note);

				message = "候位備註已更新";
			}

			default -> throw new IllegalArgumentException("不支援的候位操作");
			}

			redirect.addFlashAttribute("message", message);

		} catch (IllegalArgumentException
		        | WaitingUnavailableException
		        | SmsSendException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}

		redirect.addAttribute("tab", "waiting");

		if (seatType != null && TABLE_TYPES.contains(seatType)) {
			redirect.addAttribute("seatType", seatType);
		}

		return "redirect:/staff/booking";
	}
	
	// 只允許Console模式測試，不提供正式簡訊批次發送入口。
	@PostMapping("/booking/sms/test-reminder")
	public String testReservationReminder(
	        @RequestParam(name = "bookingToken") String token,
	        HttpSession session,
	        RedirectAttributes redirect) {

	    requireBookingEmployee(session);
	    checkBookingActionToken(session, token);

	    if (!reservationReminderScheduler.isConsoleMode()) {
	        throw new ResponseStatusException(
	                HttpStatus.FORBIDDEN,
	                "這個入口只允許Console測試模式");
	    }

	    try {
	        var result =
	                reservationReminderScheduler.sendTodayReminders();

	        redirect.addFlashAttribute(
	                "message",
	                "Console測試完成："
	                        + result.sent() + " 筆成功、"
	                        + result.skipped() + " 筆略過、"
	                        + result.failed() + " 筆失敗");

	    } catch (RuntimeException ex) {
	        redirect.addFlashAttribute(
	                "error",
	                "提醒測試未完成，請確認資料庫及Redis連線");
	    }

	    return "redirect:/staff/booking";
	}
}