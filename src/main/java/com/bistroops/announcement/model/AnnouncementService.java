package com.bistroops.announcement.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementService {

	@Autowired 
	AnnouncementRepository dao;

	public List<AnnouncementVO> getAll() {
		return dao.findAll();
	}

	public AnnouncementVO getAnnNoQuery(Integer annNo) {
		return dao.findById(annNo).orElse(null);
	}

	public void insertAnn(String annTitle, LocalDateTime annBegin, byte[] annImg, String annText) {
		AnnouncementVO ann = new AnnouncementVO();

		ann.setAnnTitle(annTitle);
		ann.setAnnBegin(annBegin);
		ann.setAnnImg(annImg);
		ann.setAnnText(annText);

		dao.save(ann);
	}

	public void updateAnn(Integer annNo, String annTitle, LocalDateTime annBegin, byte[] annImg, String annText) {
		AnnouncementVO ann = dao.findById(annNo).orElseThrow();

		ann.setAnnTitle(annTitle);
		ann.setAnnBegin(annBegin);
		ann.setAnnText(annText);

		// 沒有上傳新圖片時保留原有圖片，不覆蓋成 null
		if (annImg != null && annImg.length > 0) {
			ann.setAnnImg(annImg);
		}

		dao.save(ann);
	}

	public void deleteAnn(Integer annNo) {
		dao.deleteById(annNo);
	}
}
