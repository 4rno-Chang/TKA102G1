package com.bistroops.qa.model;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QaService {

	@Autowired 
	QaRepository qaRepo;

	public List<QaVO> getAll() {
		return qaRepo.findAll();
	}

	public QaVO getQaNoQuery(Integer qaNo) {
		return qaRepo.findById(qaNo).orElse(null);
	}

	public void insertQa(String qaTitle, String qaContent) {
		QaVO qa = new QaVO();

		qa.setQaTitle(qaTitle);
		qa.setQaContent(qaContent);

		qaRepo.save(qa);
	}

	public void updateQa(Integer qaNo, String qaTitle, String qaContent) {
		QaVO qa = qaRepo.findById(qaNo).orElseThrow();

		qa.setQaTitle(qaTitle);
		qa.setQaContent(qaContent);

		qaRepo.save(qa);
	}

	public void deleteQa(Integer qaNo) {
		qaRepo.deleteById(qaNo);
	}
}
