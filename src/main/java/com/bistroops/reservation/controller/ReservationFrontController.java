package com.bistroops.reservation.controller;

import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;
import com.bistroops.member.model.MemberService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bistroops.member.model.MemberVO;
import com.bistroops.reservation.model.ReservationService;
import com.bistroops.reservation.model.ReservationVO;
import com.bistroops.reservationdatetime.model.ReservationDatetimeService;

import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/bistroops/reservation")
public class ReservationFrontController {

	private final ReservationService reservationService;
	private final ReservationDatetimeService rsvDtService;
	private final MemberService memberService;

	public ReservationFrontController(ReservationService reservationService, ReservationDatetimeService rsvDtService,
			MemberService memberService) {

		this.reservationService = reservationService;
		this.rsvDtService = rsvDtService;
		this.memberService = memberService;
	}

	// 日期、桌型與剩餘桌數一次預載。
	// 另外接收公開電話查詢。
	@GetMapping
	public String showDate(
			@RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

			@RequestParam(name = "tableType", defaultValue = "2") Integer tableType,

			@RequestParam(name = "lookupPhone", required = false) String lookupPhone,

			Model model) {

		if (!Set.of(2, 4, 6).contains(tableType)) {
			tableType = 2;
		}

		Map<String, Object> availability = reservationService.getAvailabilitySnapshot();

		LocalDate minDate = LocalDate.parse(availability.get("minDate").toString());

		LocalDate maxDate = LocalDate.parse(availability.get("maxDate").toString());

		if (date != null && (date.isBefore(minDate) || date.isAfter(maxDate))) {

			model.addAttribute("error", "日期超出可預約範圍，請重新選擇");

			date = null;
		}

		model.addAttribute("date", date == null ? "" : date.toString());

		model.addAttribute("tableType", tableType);
		model.addAttribute("reservationAvailability", availability);

		model.addAttribute("lookupPhone", lookupPhone == null ? "" : lookupPhone.trim());

		model.addAttribute("lookupPerformed", lookupPhone != null);
		model.addAttribute("lookupList", List.of());

		if (lookupPhone != null) {
			try {
				model.addAttribute("lookupList", reservationService.lookupByPhone(lookupPhone));

			} catch (IllegalArgumentException ex) {
				model.addAttribute("lookupError", ex.getMessage());
			}
		}

		return "front/reservation/reservation_date";
	}

	// 不要求會員登入。
	@GetMapping("/contact")
	public String showContact(
			@RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

			@RequestParam(name = "tableType") Integer tableType,

			@RequestParam(name = "rsvDtNo") Long rsvDtNo,

			HttpSession session, Model model, RedirectAttributes redirectAttributes) {

		if (!Set.of(2, 4, 6).contains(tableType) || !rsvDtService.getOpenDates().contains(date)) {

			redirectAttributes.addFlashAttribute("error", "日期或桌型不正確，請重新選擇");

			return "redirect:/bistroops/reservation";
		}

		Map<String, Object> selectedSlot = reservationService.getSlots(date, tableType).stream()
				.filter(slot -> rsvDtNo.equals(slot.get("rsvDtNo")))
				.filter(slot -> Boolean.FALSE.equals(slot.get("full"))).findFirst().orElse(null);

		if (selectedSlot == null) {
			redirectAttributes.addFlashAttribute("error", "這個時段已額滿或無法預約，請重新選擇");

			redirectAttributes.addAttribute("date", date.toString());
			redirectAttributes.addAttribute("tableType", tableType);

			return "redirect:/bistroops/reservation";
		}

		// 表單驗證碼與會員登入無關。
		String token = (String) session.getAttribute("reservationFormToken");

		if (token == null) {
			token = UUID.randomUUID().toString();
			session.setAttribute("reservationFormToken", token);
		}

		// 只有已驗證且目前登入的會員可預填自己的電話。
		// 不用客人輸入的電話去帶出其他會員資料。
		if (!model.containsAttribute("phone")) {
			Object sessionMember = session.getAttribute("member");

			String phone = "";

			if (sessionMember instanceof MemberVO member && "已驗證".equals(member.getMemStatus())) {

				phone = member.getMemTel() == null ? "" : member.getMemTel();
			}

			model.addAttribute("phone", phone);
		}

		if (!model.containsAttribute("bookingName")) {
			model.addAttribute("bookingName", "");
		}

		if (!model.containsAttribute("note")) {
			model.addAttribute("note", "");
		}

		model.addAttribute("date", date.toString());
		model.addAttribute("dateText", date.toString().replace('-', '/'));
		model.addAttribute("tableType", tableType);
		model.addAttribute("rsvDtNo", rsvDtNo);
		model.addAttribute("timeText", selectedSlot.get("timeText"));
		model.addAttribute("reservationFormToken", token);

		return "front/reservation/reservation_contact";
	}

	@PostMapping("/submit")
	public String submit(@RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

			@RequestParam(name = "tableType") Integer tableType,

			@RequestParam(name = "rsvDtNo") Long rsvDtNo,

			@RequestParam(name = "phone", defaultValue = "") String phone,

			@RequestParam(name = "bookingName", defaultValue = "") String bookingName,

			@RequestParam(name = "note", defaultValue = "") String note,

			@RequestParam(name = "agree", defaultValue = "") String agree,

			@RequestParam(name = "reservationFormToken", required = false) String token,

			HttpSession session, RedirectAttributes redirectAttributes) {

		String expectedToken = (String) session.getAttribute("reservationFormToken");

		if (expectedToken == null || !expectedToken.equals(token)) {
			redirectAttributes.addFlashAttribute("error", "表單已失效，請重新選擇日期與時段");

			return "redirect:/bistroops/reservation";
		}

		try {
			if (!"yes".equals(agree)) {
				throw new IllegalArgumentException("請先同意訂位須知與個人資料使用說明");
			}

			ReservationVO reservation = reservationService.addByPhone(phone, bookingName, date, rsvDtNo, tableType,
					note);

			rememberBooking(session, reservation.getRsvNo());

			redirectAttributes.addAttribute("rsvNo", reservation.getRsvNo());

			return "redirect:/bistroops/reservation/done";

		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
			redirectAttributes.addFlashAttribute("phone", phone);
			redirectAttributes.addFlashAttribute("bookingName", bookingName);
			redirectAttributes.addFlashAttribute("note", note);

			redirectAttributes.addAttribute("date", date.toString());
			redirectAttributes.addAttribute("tableType", tableType);
			redirectAttributes.addAttribute("rsvDtNo", rsvDtNo);

			return "redirect:/bistroops/reservation/contact";
		}
	}

	// 只允許同一個瀏覽器查看自己建立的訂位。
	@GetMapping("/done")
	public String showDone(@RequestParam(name = "rsvNo") Long rsvNo, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {

		if (!ownsBooking(session, rsvNo)) {
			redirectAttributes.addFlashAttribute("error", "無法查看這筆訂位，請使用電話查詢");

			return "redirect:/bistroops/reservation";
		}

		try {
			model.addAllAttributes(reservationService.getResultForBooking(rsvNo));

			return "front/reservation/reservation_done";

		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());

			return "redirect:/bistroops/reservation";
		}
	}

	// 用 Session 保留最近20筆由這個瀏覽器建立的訂位。
	// 不新增資料庫欄位，也不登入該電話的會員。
	private void rememberBooking(HttpSession session, Long rsvNo) {

		synchronized (session) {
			Set<Long> ids = readBookingIds(session);
			ids.add(rsvNo);

			while (ids.size() > 20) {
				ids.remove(ids.iterator().next());
			}

			session.setAttribute("guestReservationIds", ids);
		}
	}

	private boolean ownsBooking(HttpSession session, Long rsvNo) {

		synchronized (session) {
			return readBookingIds(session).contains(rsvNo);
		}
	}

	private Set<Long> readBookingIds(HttpSession session) {

		Set<Long> ids = new LinkedHashSet<>();
		Object stored = session.getAttribute("guestReservationIds");

		if (stored instanceof Set<?> values) {
			for (Object value : values) {
				if (value instanceof Long id) {
					ids.add(id);
				}
			}
		}

		return ids;
	}

	@GetMapping("/mine")
	@ResponseBody
	public ResponseEntity<?> getMyReservations(HttpSession session) {

		Object stored = session.getAttribute("member");

		if (!(stored instanceof MemberVO member)) {
			return ResponseEntity.status(401).header("Cache-Control", "no-store").body(Map.of("message", "請先登入會員"));
		}

		MemberVO current = memberService.getMemNoQuery(member.getMemNo());

		if (current == null) {
			session.removeAttribute("member");

			return ResponseEntity.status(401).header("Cache-Control", "no-store")
					.body(Map.of("message", "登入已失效，請重新登入"));
		}

		boolean canManage = "已驗證".equals(member.getMemStatus()) && "已驗證".equals(current.getMemStatus());

		Map<String, Object> result = new LinkedHashMap<>();

		result.putAll(reservationService.getMemberReservationSnapshot(current.getMemNo()));

		result.put("canManage", canManage);
		result.put("csrfToken", canManage ? reservationActionToken(session, current.getMemNo()) : "");

		return ResponseEntity.ok().header("Cache-Control", "no-store").body(result);
	}

	// 確認這個登入 Session 及資料庫中的會員都已驗證。
	private MemberVO requireVerifiedReservationMember(HttpSession session) {

		Object stored = session.getAttribute("member");

		if (!(stored instanceof MemberVO member)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入會員");
		}

		MemberVO current = memberService.getMemNoQuery(member.getMemNo());

		if (current == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登入已失效");
		}

		if (!"已驗證".equals(member.getMemStatus()) || !"已驗證".equals(current.getMemStatus())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "請先完成電話驗證");
		}

		return current;
	}

	// 操作憑證與登入會員綁定，供編輯及取消使用。
	private String reservationActionToken(HttpSession session, Integer memNo) {

		synchronized (session) {
			Object owner = session.getAttribute("memberRsvTokenOwner");
			Object token = session.getAttribute("memberRsvActionToken");

			if (!memNo.equals(owner) || !(token instanceof String)) {
				token = UUID.randomUUID().toString();

				session.setAttribute("memberRsvTokenOwner", memNo);
				session.setAttribute("memberRsvActionToken", token);
			}

			return (String) token;
		}
	}

	private void checkReservationActionToken(HttpSession session, Integer memNo, String token) {

		synchronized (session) {
			Object owner = session.getAttribute("memberRsvTokenOwner");
			Object expected = session.getAttribute("memberRsvActionToken");

			if (!memNo.equals(owner) || token == null || !token.equals(expected)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "操作已失效，請重新開啟預約紀錄");
			}
		}
	}

	// 獨立編輯頁。
	@GetMapping("/edit")
	public String showEditReservation(@RequestParam(name = "rsvNo") Long rsvNo, HttpSession session, Model model) {

		MemberVO member = requireVerifiedReservationMember(session);

		ReservationVO reservation = reservationService.getOneForMember(rsvNo, member.getMemNo());

		LocalDateTime diningTime = reservation.getRsvDt().getRsvDtDatetime();

		if (!"預約".equals(reservation.getRsvStatus())
				|| !diningTime.isAfter(LocalDateTime.now(java.time.ZoneId.of("Asia/Taipei")))) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "這筆訂位已無法編輯");
		}

		model.addAttribute("rsvNo", rsvNo);
		model.addAttribute("editToken", reservationActionToken(session, member.getMemNo()));

		if (!model.containsAttribute("date")) {
			model.addAttribute("date", diningTime.toLocalDate());
		}

		if (!model.containsAttribute("tableType")) {
			model.addAttribute("tableType", reservation.getSeatType().getSeatTypeNo());
		}

		if (!model.containsAttribute("selectedRsvDtNo")) {
			model.addAttribute("selectedRsvDtNo", reservation.getRsvDt().getRsvDtNo());
		}

		if (!model.containsAttribute("bookingName")) {
			model.addAttribute("bookingName", reservation.getRsvName() == null ? "" : reservation.getRsvName());
		}

		if (!model.containsAttribute("note")) {
			model.addAttribute("note", reservation.getRsvComment() == null ? "" : reservation.getRsvComment());
		}

		model.addAttribute("originalBooking", Map.of("date", diningTime.toLocalDate().toString(), "tableType",
				reservation.getSeatType().getSeatTypeNo(), "rsvDtNo", reservation.getRsvDt().getRsvDtNo()));

		model.addAttribute("reservationAvailability", reservationService.getAvailabilitySnapshot());

		return "front/reservation/reservation_edit";
	}

	// 儲存編輯結果。
	@PostMapping("/edit")
	public String saveEditReservation(@RequestParam(name = "rsvNo") Long rsvNo,
			@RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(name = "tableType") Integer tableType, @RequestParam(name = "rsvDtNo") Long rsvDtNo,
			@RequestParam(name = "bookingName", defaultValue = "") String bookingName,
			@RequestParam(name = "note", defaultValue = "") String note,
			@RequestParam(name = "editToken") String editToken, HttpSession session,
			RedirectAttributes redirectAttributes) {

		MemberVO member = requireVerifiedReservationMember(session);

		checkReservationActionToken(session, member.getMemNo(), editToken);

		try {
			reservationService.editForMember(rsvNo, member.getMemNo(), date, rsvDtNo, tableType, bookingName, note);

			redirectAttributes.addFlashAttribute("startTab", "reservations");
			redirectAttributes.addFlashAttribute("toastMsg", "訂位已更新");

			return "redirect:/bistroops/reservation";

		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());

			redirectAttributes.addFlashAttribute("date", date);
			redirectAttributes.addFlashAttribute("tableType", tableType);
			redirectAttributes.addFlashAttribute("selectedRsvDtNo", rsvDtNo);
			redirectAttributes.addFlashAttribute("bookingName", bookingName);
			redirectAttributes.addFlashAttribute("note", note);

			redirectAttributes.addAttribute("rsvNo", rsvNo);

			return "redirect:/bistroops/reservation/edit";
		}
	}

	// 取消訂位：保留資料，不實際刪除資料列。
	@PostMapping("/cancel")
	@ResponseBody
	public ResponseEntity<?> cancelMyReservation(@RequestParam(name = "rsvNo") Long rsvNo,
			@RequestParam(name = "csrfToken") String csrfToken, HttpSession session) {

		MemberVO member = requireVerifiedReservationMember(session);

		checkReservationActionToken(session, member.getMemNo(), csrfToken);

		try {
			reservationService.cancelForMember(rsvNo, member.getMemNo());

			return ResponseEntity.ok(Map.of("message", "訂位已取消"));

		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
		}
	}
}