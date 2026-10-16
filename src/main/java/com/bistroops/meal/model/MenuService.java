package com.bistroops.meal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bistroops.promote.model.PromoPriceDTO;

@Service
public class MenuService {
	
	@Autowired
	private MenuRepository menuRepository;
	
	//所有上架的菜，套上活動價
	@Transactional(readOnly = true)
	public List<MenuMealCardDTO> getMenuCards(Map<Integer, PromoPriceDTO> activePrices){
		List <MenuMealCardDTO> cards = menuRepository.findMenuCards(MealEnum.ONSALE);
		for (MenuMealCardDTO c : cards) {
			PromoPriceDTO p = activePrices.get(c.getMealNo());
			if (p != null) c.applyPromo(p);      // 有活動的菜換成活動價、記下活動名稱
		}
  
		return cards;
	}
	
	//依類別分組
	public List<MenuSectionDTO> getMenuSections(List<MenuMealCardDTO> cards) {
	    List<MenuSectionDTO> sections = new ArrayList<>();
	    MenuSectionDTO current = null;
	    
	    for (MenuMealCardDTO c : cards) {
	        if (current == null || !current.getMealtypeNo().equals(c.getMealtypeNo())) {
	            current = new MenuSectionDTO(c.getMealtypeNo(), c.getMealtypeName());
	            sections.add(current);
	        }
	        current.getMeals().add(c);
	    }
	    return sections;
	}
	
	//挑出有活動的菜品
	public List<MenuMealCardDTO> getPromoMeals(List<MenuMealCardDTO> cards) {
    if (cards == null) {
        return Collections.emptyList();
    }

    return cards.stream()
    			.filter(c -> c.getPromoName() != null)
    			.collect(Collectors.toList());
	}
		
    public byte[] getMealImg(Integer mealNo) {
    	return menuRepository.findImgByMealNo(mealNo);
    }
}
