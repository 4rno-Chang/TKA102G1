package com.bistroops.reservation.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.bistroops.reservation.model.ReservationService;
import com.bistroops.reservationdatetime.model.ReservationDatetimeService;

import java.util.Map;

import com.bistroops.member.model.MemberVO;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.bistroops.reservation.model.ReservationVO;

@Controller
@RequestMapping("/bistroops/reservation")
public class ReservationFrontController {

	private final ReservationService reservationService;
	private final ReservationDatetimeService rsvDtService;

	public ReservationFrontController(ReservationService reservationService, ReservationDatetimeService rsvDtService) {

		this.reservationService = reservationService;
		this.rsvDtService = rsvDtService;
	}

	@RequestMapping(value = "", method = RequestMethod.GET)
	public String showDate(
			@RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

			@RequestParam(name = "tableType", defaultValue = "2") Integer tableType,

			Model model) {

		if (!Set.of(2, 4, 6).contains(tableType)) {
			tableType = 2;
		}

		List<LocalDate> openDates = rsvDtService.getOpenDates();

		if (date != null && !openDates.contains(date)) {
			model.addAttribute("error", "這一天沒有開放可選時段");
			date = null;
		}

		String dateStrings = openDates.stream().map(LocalDate::toString).collect(Collectors.joining(","));

		model.addAttribute("date", date == null ? "" : date.toString());

		model.addAttribute("dateText", date == null ? "尚未選擇" : date.toString().replace('-', '/'));

		model.addAttribute("tableType", tableType);
		model.addAttribute("openDates", dateStrings);
		model.addAttribute("minDate", LocalDate.now().toString());
		model.addAttribute("maxDate", LocalDate.now().plusMonths(3).toString());

		model.addAttribute("slots", reservationService.getSlots(date, tableType));

		return "front/reservation/reservation_date";
	}

	@RequestMapping(value = "/contact", method = RequestMethod.GET)
	public String showContact(
			@RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

			@RequestParam(name = "tableType") Integer tableType, @RequestParam(name = "rsvDtNo") Long rsvDtNo,
			HttpSession session, Model model, RedirectAttributes redirectAttributes) {

		MemberVO member = (MemberVO) session.getAttribute("member");

		if (member == null) {
			redirectAttributes.addFlashAttribute("error", "請先登入會員，再進行訂位。");
			return "redirect:/bistroops/reservation";
		}

		if (!Set.of(2, 4, 6).contains(tableType) || !rsvDtService.getOpenDates().contains(date)) {

			redirectAttributes.addFlashAttribute("error", "日期或桌型不正確，請重新選擇。");
			return "redirect:/bistroops/reservation";
		}

		// 使用後端查出的時段，確認編號屬於選定日期且仍有空位。
		Map<String, Object> selectedSlot = reservationService.getSlots(date, tableType).stream()
				.filter(slot -> rsvDtNo.equals(slot.get("rsvDtNo")))
				.filter(slot -> Boolean.FALSE.equals(slot.get("full"))).findFirst().orElse(null);

		if (selectedSlot == null) {
			redirectAttributes.addFlashAttribute("error", "這個時段已額滿或無法預約，請重新選擇。");

			redirectAttributes.addAttribute("date", date.toString());
			redirectAttributes.addAttribute("tableType", tableType);

			return "redirect:/bistroops/reservation";
		}

		model.addAttribute("member", member);
		model.addAttribute("date", date.toString());
		model.addAttribute("dateText", date.toString().replace('-', '/'));
		model.addAttribute("tableType", tableType);
		model.addAttribute("rsvDtNo", rsvDtNo);
		model.addAttribute("timeText", selectedSlot.get("timeText"));

		return "front/reservation/reservation_contact";
	}

	@RequestMapping(value = "/submit", method = RequestMethod.POST)
	public String submit(@RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

			@RequestParam(name = "tableType") Integer tableType, @RequestParam(name = "rsvDtNo") Long rsvDtNo,
			@RequestParam(name = "note", defaultValue = "") String note, HttpSession session,
			RedirectAttributes redirectAttributes) {

		MemberVO member = (MemberVO) session.getAttribute("member");

		if (member == null) {
			redirectAttributes.addFlashAttribute("error", "請先登入會員");
			return "redirect:/bistroops/reservation";
		}

		try {
			ReservationVO reservation = reservationService.addForMember(member.getMemNo(), date, rsvDtNo, tableType,
					note);

			redirectAttributes.addAttribute("rsvNo", reservation.getRsvNo());

			return "redirect:/bistroops/reservation/done";

		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());

			redirectAttributes.addAttribute("date", date.toString());
			redirectAttributes.addAttribute("tableType", tableType);

			return "redirect:/bistroops/reservation";
		}
	}

	@RequestMapping(value = "/done", method = RequestMethod.GET)
	public String showDone(@RequestParam(name = "rsvNo") Long rsvNo, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {

		MemberVO member = (MemberVO) session.getAttribute("member");

		if (member == null) {
			redirectAttributes.addFlashAttribute("error", "請先登入會員");
			return "redirect:/bistroops/reservation";
		}

		try {
			model.addAllAttributes(reservationService.getResultForMember(rsvNo, member.getMemNo()));

			return "front/reservation/reservation_done";

		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/bistroops/reservation";
		}
	}

}