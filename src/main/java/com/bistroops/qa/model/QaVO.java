package com.bistroops.qa.model;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "qa")
public class QaVO implements java.io.Serializable{
/*
qa_no INT UNSIGNED AUTO_INCREMENT COMMENT '編號',
qa_title VARCHAR(30) COMMENT '標題',
qa_content MEDIUMTEXT COMMENT '內容',
qa_clicks INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '點擊次數',（G1 v1_4 沒有，05 新增，寫在 createTable_demo.sql）
*/
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "qa_no")
	private Integer qaNo;
	
	@Column(name = "qa_title")
	private String qaTitle;
	
	@Column(name="qa_content", columnDefinition = "mediumtext")
	private String qaContent;

	// 前台展開題目的次數；預設 0，後台新增題目時不會寫入 NULL
	@Column(name = "qa_clicks")
	private Integer qaClicks = 0;

	public Integer getQaNo() {
		return qaNo;
	}

	public void setQaNo(Integer qaNo) {
		this.qaNo = qaNo;
	}

	public String getQaTitle() {
		return qaTitle;
	}

	public void setQaTitle(String qaTitle) {
		this.qaTitle = qaTitle;
	}

	public String getQaContent() {
		return qaContent;
	}

	public void setQaContent(String qaContent) {
		this.qaContent = qaContent;
	}

	public Integer getQaClicks() {
		return qaClicks;
	}

	public void setQaClicks(Integer qaClicks) {
		this.qaClicks = qaClicks;
	}
	
	
			  
	
}
