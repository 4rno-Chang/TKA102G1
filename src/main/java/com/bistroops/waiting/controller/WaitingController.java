package com.bistroops.waiting.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;

import com.bistroops.member.model.MemberVO;
import com.bistroops.waiting.model.WaitingService;
import com.bistroops.waiting.model.WaitingVO;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/bistroops/waiting")
public class WaitingController {

	private final WaitingService waitingService;

	public WaitingController(WaitingService waitingService) {
		this.waitingService = waitingService;
	}

	@RequestMapping(value = "/join", method = RequestMethod.GET)
	public String showJoin(@RequestParam(name = "tableType", defaultValue = "2") Integer tableType, HttpSession session,
			Model model) {

		if (!Set.of(2, 4, 6).contains(tableType)) {
			tableType = 2;
		}

		prepareJoin(model, session, tableType);
		return "front/waiting/waiting_join";
	}

	@RequestMapping(value = "/join", method = RequestMethod.POST)
	public String join(@RequestParam(name = "tableType", required = false) Integer tableType,

			@RequestParam(name = "name", defaultValue = "") String name,

			@RequestParam(name = "phone", defaultValue = "") String phone,

			@RequestParam(name = "note", defaultValue = "") String note,

			@RequestParam(name = "waitingCsrfToken", required = false) String csrfToken,

			HttpSession session, Model model, RedirectAttributes redirectAttributes) {

		String expected = (String) session.getAttribute("waitingCsrfToken");

		if (expected == null || !expected.equals(csrfToken)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "表單驗證失敗，請重新開啟取號頁面");
		}

		MemberVO member = (MemberVO) session.getAttribute("member");

		Integer memNo = member == null ? null : member.getMemNo();

		try {
			WaitingVO waiting = waitingService.addWaiting(memNo, tableType, phone, name, note);

			allowedTickets(session).add(waiting.getWaitingNo());

			redirectAttributes.addAttribute("waitingNo", waiting.getWaitingNo());

			return "redirect:/bistroops/waiting/ticket";

		} catch (IllegalArgumentException ex) {
			model.addAttribute("error", ex.getMessage());
			model.addAttribute("name", name);
			model.addAttribute("phone", phone);
			model.addAttribute("note", note);

			Integer selected = tableType != null && Set.of(2, 4, 6).contains(tableType) ? tableType : 2;

			prepareJoin(model, session, selected);
			return "front/waiting/waiting_join";
		}
	}

	@RequestMapping(value = "/ticket", method = RequestMethod.GET)
	public String showTicket(@RequestParam("waitingNo") Long waitingNo, HttpSession session, Model model) {

		// 本階段只允許從同一個瀏覽器查看自己剛取得的號碼牌。
		if (!allowedTickets(session).contains(waitingNo)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "無法查看這張號碼牌");
		}

		WaitingVO waiting = waitingService.getOneWaiting(waitingNo);

		boolean active = WaitingService.WAITING.equals(waiting.getWaitingStatus())
				|| WaitingService.NOTIFIED.equals(waiting.getWaitingStatus());

		model.addAttribute("waiting", waiting);
		model.addAttribute("active", active);
		model.addAttribute("ahead", active ? waitingService.countAhead(waitingNo) : 0);

		return "front/waiting/waiting_ticket";
	}

	private void prepareJoin(Model model, HttpSession session, Integer tableType) {

		if (session.getAttribute("waitingCsrfToken") == null) {
			session.setAttribute("waitingCsrfToken", UUID.randomUUID().toString());
		}

		List<Map<String, Object>> queues = List.of(queue(2), queue(4), queue(6));

		// 目前先開放測試；營業時間與停止取號規則之後集中處理。
		model.addAttribute("open", true);
		model.addAttribute("queues", queues);
		model.addAttribute("tableType", tableType);
		model.addAttribute("waitingCsrfToken", session.getAttribute("waitingCsrfToken"));
	}

	private Map<String, Object> queue(Integer tableType) {
		return Map.of("tableType", tableType, "waitingCount", waitingService.countActiveBySeatType(tableType),
				"estimate", "請洽現場人員");
	}

	@SuppressWarnings("unchecked")
	private Set<Long> allowedTickets(HttpSession session) {
		Set<Long> tickets = (Set<Long>) session.getAttribute("waitingTickets");

		if (tickets == null) {
			tickets = new HashSet<>();
			session.setAttribute("waitingTickets", tickets);
		}

		return tickets;
	}
}
