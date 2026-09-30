package com.bistroops.announcement.model;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "announcement")
public class AnnouncementVO implements java.io.Serializable{
/*
ann_no INT UNSIGNED AUTO_INCREMENT COMMENT '編號',
ann_title VARCHAR(30) COMMENT '標題',
ann_begin DATETIME COMMENT '公告時間',
ann_img MEDIUMBLOB COMMENT '推播圖',
ann_text MEDIUMTEXT COMMENT '文字內容',
*/
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ann_no")
	private Integer annNo;
	
	@Column(name = "ann_title")
	private String annTitle;
	
	@Column(name = "ann_begin")
	private LocalDateTime annBegin;
	
	@Lob
	@Column(name = "ann_img", columnDefinition = "mediumblob")
	private byte[] annImg;
	
	@Lob
	@Column(name = "ann_text", columnDefinition = "mediumtext")
	private String annText;

	public Integer getAnnNo() {
		return annNo;
	}

	public void setAnnNo(Integer annNo) {
		this.annNo = annNo;
	}

	public String getAnnTitle() {
		return annTitle;
	}

	public void setAnnTitle(String annTitle) {
		this.annTitle = annTitle;
	}

	public LocalDateTime getAnnBegin() {
		return annBegin;
	}

	public void setAnnBegin(LocalDateTime annBegin) {
		this.annBegin = annBegin;
	}

	public byte[] getAnnImg() {
		return annImg;
	}

	public void setAnnImg(byte[] annImg) {
		this.annImg = annImg;
	}

	public String getAnnText() {
		return annText;
	}

	public void setAnnText(String annText) {
		this.annText = annText;
	}
	
	
}
