package com.bistroops.waiting.controller;

import java.time.LocalTime;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bistroops.employee.model.EmployeeService;
import com.bistroops.waiting.model.WaitingRedisService;
import com.bistroops.waiting.model.WaitingTicket;
import com.bistroops.waiting.model.WaitingUnavailableException;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/bistroops/waiting")
public class WaitingController {

	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Taipei");
	private static final List<Integer> TABLE_TYPES = List.of(2, 4, 6);

	private final WaitingRedisService waitingRedisService;
	private final EmployeeService employeeService;

	@Value("${app.waiting.join-url}")
	private String waitingJoinUrl;

	public WaitingController(WaitingRedisService waitingRedisService, EmployeeService employeeService) {

		this.waitingRedisService = waitingRedisService;
		this.employeeService = employeeService;
	}

	@GetMapping("/join")
	public String showJoin(@RequestParam(name = "tableType", defaultValue = "2") Integer tableType, HttpSession session,
			Model model) {

		model.addAttribute("tableType", TABLE_TYPES.contains(tableType) ? tableType : 2);

		model.addAttribute("waitingCsrfToken", csrfToken(session));

		try {
			model.addAttribute("queues", queues());
			model.addAttribute("open", true);

		} catch (WaitingUnavailableException ex) {
			model.addAttribute("queues", List.of());
			model.addAttribute("open", false);
			model.addAttribute("error", ex.getMessage());
		}

		return "front/waiting/waiting_join";
	}

	@PostMapping("/join")
	public String join(@RequestParam(name = "tableType") Integer tableType,
			@RequestParam(name = "phone", defaultValue = "") String phone,
			@RequestParam(name = "name", defaultValue = "") String name,
			@RequestParam(name = "note", defaultValue = "") String note,
			@RequestParam(name = "waitingCsrfToken", defaultValue = "") String token, HttpSession session,
			RedirectAttributes redirect) {

		try {
			checkToken(session, token);

		} catch (ResponseStatusException ex) {
			if (ex.getStatusCode().value() != 403) {
				throw ex;
			}

			redirect.addFlashAttribute("error", "頁面已失效，已重新載入，請確認資料後再次送出");

			redirect.addFlashAttribute("phone", phone);
			redirect.addFlashAttribute("name", name);
			redirect.addFlashAttribute("note", note);
			redirect.addAttribute("tableType", tableType);

			return "redirect:/bistroops/waiting/join";
		}

		try {
			WaitingTicket ticket = waitingRedisService.takeTicket(tableType, phone, name, note);

			session.setAttribute(ticketSessionKey(ticket.date(), ticket.tableType(), ticket.ticketId()),
					ticket.accessToken());

			redirect.addAttribute("date", ticket.date());
			redirect.addAttribute("tableType", ticket.tableType());
			redirect.addAttribute("ticketId", ticket.ticketId());

			return "redirect:/bistroops/waiting/ticket";

		} catch (IllegalArgumentException | WaitingUnavailableException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
			redirect.addFlashAttribute("phone", phone);
			redirect.addFlashAttribute("name", name);
			redirect.addFlashAttribute("note", note);
			redirect.addAttribute("tableType", tableType);

			return "redirect:/bistroops/waiting/join";
		}
	}

	@GetMapping("/ticket")
	public String showTicket(@RequestParam(name = "date") String date,
			@RequestParam(name = "tableType") Integer tableType, @RequestParam(name = "ticketId") String ticketId,
			HttpSession session, Model model, RedirectAttributes redirect) {

		try {
			WaitingTicket ticket = ownedTicket(session, date, tableType, ticketId);

			model.addAttribute("ticket", ticketView(ticket));
			model.addAttribute("ahead",
					waitingRedisService.isActive(ticket) ? waitingRedisService.countAhead(ticket) : 0);

			model.addAttribute("waitingCsrfToken", csrfToken(session));

			return "front/waiting/waiting_ticket";

		} catch (IllegalArgumentException | WaitingUnavailableException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());

			return "redirect:/bistroops/waiting";
		}
	}

	@PostMapping("/cancel")
	public String cancel(@RequestParam(name = "date") String date, @RequestParam(name = "tableType") Integer tableType,
			@RequestParam(name = "ticketId") String ticketId, @RequestParam(name = "waitingCsrfToken") String token,
			HttpSession session, RedirectAttributes redirect) {

		checkToken(session, token);

		try {
			WaitingTicket ticket = ownedTicket(session, date, tableType, ticketId);

			waitingRedisService.cancelByTicket(date, tableType, ticketId, ticket.accessToken());

			redirect.addFlashAttribute("message", "候位已取消");

		} catch (IllegalArgumentException | WaitingUnavailableException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}

		redirect.addAttribute("date", date);
		redirect.addAttribute("tableType", tableType);
		redirect.addAttribute("ticketId", ticketId);

		return "redirect:/bistroops/waiting/ticket";
	}

	@GetMapping
	public String showStatus(@RequestParam(name = "lookupPhone", required = false) String lookupPhone, Model model) {

		model.addAttribute("manage", false);
		prepareStatus(model);

		model.addAttribute("lookupPhone", lookupPhone == null ? "" : lookupPhone.trim());

		model.addAttribute("lookupPerformed", lookupPhone != null);
		model.addAttribute("lookupTickets", List.of());

		if (lookupPhone != null) {
			try {
				String phone = lookupPhone.trim();

				if (!phone.matches("09[0-9]{8}")) {
					throw new IllegalArgumentException("請輸入09開頭的10碼手機號碼");
				}

				List<Map<String, Object>> tickets = TABLE_TYPES.stream()
						.flatMap(type -> waitingRedisService.getTodayTickets(type).stream())
						.filter(ticket -> phone.equals(ticket.phone()))
						.sorted(java.util.Comparator.comparingLong(WaitingTicket::takenAt)).map(this::ticketView)
						.toList();

				model.addAttribute("lookupTickets", tickets);

			} catch (IllegalArgumentException | WaitingUnavailableException ex) {
				model.addAttribute("error", ex.getMessage());
			}
		}

		return "front/waiting/waiting_status";
	}

	// 組員測試用管理入口，必須登入有效員工帳號。
	@GetMapping("/manage")
	public String showManage(HttpSession session, Model model) {

		requireEmployee(session);

		model.addAttribute("manage", true);
		model.addAttribute("waitingCsrfToken", csrfToken(session));
		model.addAttribute("staffTickets", List.of());

		prepareStatus(model);

		try {
			List<Map<String, Object>> tickets = TABLE_TYPES.stream()
					.flatMap(type -> waitingRedisService.getTodayTickets(type).stream()).map(this::ticketView).toList();

			model.addAttribute("staffTickets", tickets);

		} catch (WaitingUnavailableException ex) {
			model.addAttribute("open", false);
			model.addAttribute("error", ex.getMessage());
		}

		return "front/waiting/waiting_status";
	}

	@PostMapping("/manage/action")
	public String manageAction(@RequestParam(name = "operation") String operation,
			@RequestParam(name = "tableType") Integer tableType,
			@RequestParam(name = "ticketId", defaultValue = "") String ticketId,
			@RequestParam(name = "note", defaultValue = "") String note,
			@RequestParam(name = "waitingCsrfToken") String token, HttpSession session, RedirectAttributes redirect) {

		requireEmployee(session);
		checkToken(session, token);

		try {
			switch (operation) {
			case "NOTIFY" -> waitingRedisService.notifyNext(tableType);
			case "MISS" -> waitingRedisService.missWaiting(tableType, ticketId);
			case "SEAT" -> waitingRedisService.seatWaiting(tableType, ticketId);
			case "CANCEL" -> waitingRedisService.cancelByStaff(tableType, ticketId);
			case "COMMENT" -> waitingRedisService.updateCommentByStaff(tableType, ticketId, note);
			default -> throw new IllegalArgumentException("不支援的候位操作");
			}

			redirect.addFlashAttribute("message", "候位資料已更新");

		} catch (IllegalArgumentException | WaitingUnavailableException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}

		return "redirect:/bistroops/waiting/manage";
	}

	private void prepareStatus(Model model) {

		model.addAttribute("refreshTime", LocalTime.now(BUSINESS_ZONE).format(DateTimeFormatter.ofPattern("HH:mm:ss")));

		try {
			model.addAttribute("queues", queues());
			model.addAttribute("open", true);

		} catch (WaitingUnavailableException ex) {
			model.addAttribute("queues", List.of());
			model.addAttribute("open", false);
			model.addAttribute("error", ex.getMessage());
		}
	}

	private List<Map<String, Object>> queues() {

		return TABLE_TYPES.stream().map(type -> {
			Map<String, Object> queue = new LinkedHashMap<>(waitingRedisService.getPublicSummary(type));

			queue.put("estimate", "請洽現場人員");

			return queue;
		}).toList();
	}

	private Map<String, Object> ticketView(WaitingTicket ticket) {

		Map<String, Object> result = new LinkedHashMap<>();

		result.put("date", ticket.date());
		result.put("tableType", ticket.tableType());
		result.put("ticketId", ticket.ticketId());
		result.put("number", ticket.getTicketDisplay());
		result.put("name", ticket.name());
		result.put("phone", ticket.phone());
		result.put("note", ticket.note());
		result.put("status", ticket.status());
		result.put("statusText", ticket.getStatusText());
		result.put("notifyDeadline", ticket.notifyDeadline());
		result.put("active", waitingRedisService.isActive(ticket));

		// 不放入號碼牌操作憑證。
		return result;
	}

	private WaitingTicket ownedTicket(HttpSession session, String date, Integer tableType, String ticketId) {

		Object stored = session.getAttribute(ticketSessionKey(date, tableType, ticketId));

		if (!(stored instanceof String accessToken)) {
			throw new IllegalArgumentException("無法查看這張號碼牌，請使用電話查詢");
		}

		return waitingRedisService.getTicket(date, tableType, ticketId, accessToken);
	}

	private String ticketSessionKey(String date, Integer tableType, String ticketId) {

		return "waitingRedisTicket:" + date + ":" + tableType + ":" + ticketId;
	}

	private String csrfToken(HttpSession session) {

		synchronized (session) {
			Object stored = session.getAttribute("waitingCsrfToken");

			if (stored instanceof String token) {
				return token;
			}

			String token = UUID.randomUUID().toString();
			session.setAttribute("waitingCsrfToken", token);

			return token;
		}
	}

	private void checkToken(HttpSession session, String token) {

		Object expected = session.getAttribute("waitingCsrfToken");

		if (!(expected instanceof String) || token == null || !token.equals(expected)) {

			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "表單已失效，請重新開啟頁面");
		}
	}

	private void requireEmployee(HttpSession session) {

		Object stored = session.getAttribute("employeeNo");

		if (!(stored instanceof Integer employeeNo)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "請先登入員工帳號");
		}

		var employee = employeeService.getEmployeeById(employeeNo).orElse(null);

		if (employee == null || !"1".equals(employee.getEmpStatus())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "員工帳號無法執行操作");
		}
	}

	// 產生可下載、列印的候位 QR Code。
	@GetMapping(value = "/qrcode.png", produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> waitingQrCode() {

		String targetUrl = waitingJoinUrl.trim();

		if (targetUrl.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "尚未設定候位取號網址");
		}

		Map<EncodeHintType, Object> hints = Map.of(EncodeHintType.CHARACTER_SET, "UTF-8",
				EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 4);

		try {
			BitMatrix matrix = new QRCodeWriter().encode(targetUrl, BarcodeFormat.QR_CODE, 600, 600, hints);

			BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);

			for (int y = 0; y < matrix.getHeight(); y++) {
				for (int x = 0; x < matrix.getWidth(); x++) {
					image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
				}
			}

			byte[] png;

			try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {

				if (!ImageIO.write(image, "png", output)) {
					throw new IOException("無法產生 PNG 圖片");
				}

				png = output.toByteArray();
			}

			return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).header("Cache-Control", "no-store")
					.header("Content-Disposition", "inline; filename=\"bistroops-waiting-qrcode.png\"").body(png);

		} catch (WriterException | IOException ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "候位 QR Code 產生失敗", ex);
		}
	}
}