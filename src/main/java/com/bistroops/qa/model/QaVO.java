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
*/
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "qa_no")
	private Integer qaNo;
	
	@Column(name = "qa_title")
	private String qaTitle;
	
	@Column(name="qa_content", columnDefinition = "mediumtext")
	private String qaContent;

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
	
	
			  
	
}
