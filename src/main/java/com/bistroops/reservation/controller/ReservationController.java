package com.bistroops.reservation.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bistroops.reservation.model.ReservationService;

@Controller
@RequestMapping("/admin/rsv")
public class ReservationController {

	private final ReservationService rsvService;

	public ReservationController(ReservationService rsvService) {
		this.rsvService = rsvService;
	}

	@GetMapping
	public String list(
			@RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

			@RequestParam(name = "status", required = false) String status,

			Model model) {

		model.addAttribute("rsvList", rsvService.search(date, status));

		model.addAttribute("selectedDate", date == null ? "" : date.toString());

		model.addAttribute("selectedStatus", status == null ? "" : status.trim());

		return "admin/rsv/listAllRsv";
	}
}