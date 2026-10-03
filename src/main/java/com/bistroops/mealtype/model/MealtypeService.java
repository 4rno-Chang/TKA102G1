package com.bistroops.mealtype.model;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MealtypeService {
	@Autowired 
	MealtypeRepository dao;
	
	public List<MealtypeVO> getAllMealtype(){
		return dao.findAll();
	}

	public MealtypeVO insertMealtype(String mealtypeName) {
		MealtypeVO mealtype = new MealtypeVO();
		mealtype.setMealtypeName(mealtypeName);
		return dao.save(mealtype);
	}


	public void updateMealtype(Integer mealTypeNo , String mealtypeName) {
		MealtypeVO mealtype = dao.findById(mealTypeNo).orElseThrow();
		mealtype.setMealtypeName(mealtypeName);
		
		dao.save(mealtype);
	}

	public void deleteMealtype(Integer mealTypeNo) {
		dao.deleteById(mealTypeNo);
	}


}
