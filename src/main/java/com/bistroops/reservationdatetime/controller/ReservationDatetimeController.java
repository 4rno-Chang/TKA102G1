package com.bistroops.reservationdatetime.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bistroops.reservationdatetime.model.ReservationDatetimeService;

@Controller
@RequestMapping("/admin/rsvdt")
public class ReservationDatetimeController {

	private final ReservationDatetimeService rsvDtService;

	public ReservationDatetimeController(ReservationDatetimeService rsvDtService) {
		this.rsvDtService = rsvDtService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("rsvDtList", rsvDtService.getAll());

		return "admin/rsvdt/listAllRsvDt";
	}
}
