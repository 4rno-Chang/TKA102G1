package com.bistroops.meal.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bistroops.meal.model.MenuCoverRedisService;
import com.bistroops.meal.model.MenuMealCardDTO;
import com.bistroops.meal.model.MenuService;
import com.bistroops.promote.model.PromoteService;

// 前台「菜單」頁：/bistroops/menu → templates/front/menu/menu.html（上架的菜、活動價、依類別分組）
@Controller
@RequestMapping("/bistroops")
public class FrontMenuController {
	
	@Autowired 
	private MenuService menuService;
	@Autowired
	private MenuCoverRedisService menuCoverRedisService;
	
	@Autowired private PromoteService promoteService;

	@GetMapping("/menu")
	public String menu(Model model) {
		List<MenuMealCardDTO> cards = menuService.getMenuCards(promoteService.getActivePrices());
		
		model.addAttribute("menuSections", menuService.getMenuSections(cards));
		
		model.addAttribute("menuCovers", menuCoverRedisService.getCovers());
		
		return "front/menu/menu";
	}
}
