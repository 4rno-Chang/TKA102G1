package com.bistroops.reservation.controller;

import java.time.LocalDate;
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

@Controller
@RequestMapping("/bistroops/reservation")
public class ReservationFrontController {

	private final ReservationService reservationService;
	private final ReservationDatetimeService rsvDtService;

	public ReservationFrontController(ReservationService reservationService, ReservationDatetimeService rsvDtService) {

		this.reservationService = reservationService;
		this.rsvDtService = rsvDtService;
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
}