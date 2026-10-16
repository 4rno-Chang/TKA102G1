package com.bistroops.member.controller;

import java.time.LocalDate;
import com.bistroops.member.model.MemberRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class StaffMemberController {
    private final MemberRepository memberRepository;

    public StaffMemberController(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public record MemberRow(String name, String phone, LocalDate birth, String mail, String barcode) {}

    @GetMapping("/staff/members")
    public String members(@RequestParam(defaultValue = "") String name,
                          @RequestParam(defaultValue = "") String phone, Model model) {
        name = name.trim();
        phone = phone.trim();
        model.addAttribute("searchName", name);
        model.addAttribute("searchPhone", phone);
        model.addAttribute("hasSearch", !name.isEmpty() || !phone.isEmpty());
        model.addAttribute("members", memberRepository.searchForStaff(name, phone).stream()
                .map(member -> new MemberRow(member.getMemName(), member.getMemTel(), member.getMemBirth(),
                        member.getMemMail(), member.getMemBarcode())).toList());
        return "staff/members";
    }
}
