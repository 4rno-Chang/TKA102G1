package com.bistroops.qa.model;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QaService {

	// 前台的排序：點擊多的在前；次數一樣時，新的（編號大的）在前
	private static final Sort POPULAR_ORDER = Sort.by(Sort.Direction.DESC, "qaClicks")
			.and(Sort.by(Sort.Direction.DESC, "qaNo"));

	@Autowired
	QaRepository qaRepo;

	public List<QaVO> getAll() {
		return qaRepo.findAll();
	}

	// 首頁常見問題：點擊最多的 count 題
	public List<QaVO> getPopular(int count) {
		return qaRepo.findAll(PageRequest.of(0, count, POPULAR_ORDER)).getContent();
	}

	// 前台常見問題頁：全部，依點擊次數排
	public List<QaVO> getAllPopularFirst() {
		return qaRepo.findAll(POPULAR_ORDER);
	}

	// 前台展開一題時呼叫，點擊次數 +1
	@Transactional
	public void addClick(Integer qaNo) {
		qaRepo.addClick(qaNo);
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
