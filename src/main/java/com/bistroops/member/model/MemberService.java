package com.bistroops.member.model;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MemberService {
	
	@Autowired
	MemberRepository dao;
	
	//查詢全部會員
	public List<MemberVO> getAll() {
		return dao.findAll();
	}
	
	//依會員編號查詢
	public MemberVO getMemNoQuery(Integer memNo) {
		return dao.findById(memNo).orElse(null);
	}
	
	//註冊成會員 用手機號碼註冊 所以只寫上手機號碼及密碼
	public boolean insertMember(String memTel,  String memPassword){
		
		//先檢查手機號碼是否已註冊
		if (isTelRegistered(memTel)) {
			
			// 手機號碼已註冊，返回 false
		    return false; 
		}
		
		// 尚未註冊，建立新會員
		MemberVO member = new MemberVO();
		
		member.setMemTel(memTel);
		member.setMemPassword(memPassword);
		
		dao.save(member);
		
		// 註冊成功，返回 true
		return true; 
	}
	
	//查詢此手機是否已註冊
	public boolean isTelRegistered(String memTel) {
		
	    MemberVO existingMember = dao.findByMemTel(memTel);
	    
	    if (existingMember != null) {
	    	
	    	// 手機號碼已註冊
	        return true; 
	    } else {
	    	
	    	// 手機號碼未註冊
	        return false; 
	    }

	}
	
	//會員修改個人資料
	public boolean updateMember(Integer memNo, String memName, String memMail, String memBarcode, String memTag, LocalDate memBirth) {
		
		// 依目前登入會員的會員編號，取得該會員原本的資料
		MemberVO member = dao.findById(memNo).orElse(null);
		
		// 防呆：若會員資料不存在，則不進行更新
		if (member == null) {
			return false; // 找不到會員
		}
			
			// 更新會員資料
			member.setMemName(memName);
			member.setMemMail(memMail);
			member.setMemBarcode(memBarcode);
			member.setMemTag(memTag);
			member.setMemBirth(memBirth);   
			
			dao.save(member);
			
			return true; // 更新成功
		}
	
	//會員登入
	public MemberVO login(String memTel, String memPassword) {
		
		// 依手機號碼查詢會員資料
		MemberVO member = dao.findByMemTel(memTel);
		
		// 防呆：若會員資料不存在，則登入失敗
		if (member == null) {
			
			// 找不到會員
			return null; 
		}
		
		// 驗證密碼是否正確
		if (member.getMemPassword().equals(memPassword)) {
			
			// 登入成功，回傳會員資料
			return member; 
		} else {
			
			// 密碼錯誤，登入失敗
			return null; 
		}
	}
	
	// 會員修改密碼
	public boolean changePassword(Integer memNo, String newPassword) {

	    // 依會員編號取得會員資料
	    MemberVO member = dao.findById(memNo).orElse(null);

	    // 找不到會員
	    if (member == null) {
	        return false;
	    }

	    // 修改成新密碼
	    member.setMemPassword(newPassword);

	    // 儲存修改
	    dao.save(member);

	    return true;
	}
	
	// 忘記密碼－重設密碼
	public boolean resetPassword(String memTel, String newPassword) {

	    // 依手機號碼取得會員資料
	    MemberVO member = dao.findByMemTel(memTel);

	    // 找不到會員
	    if (member == null) {
	        return false;
	    }

	    // 設定新密碼
	    member.setMemPassword(newPassword);

	    // 儲存修改
	    dao.save(member);

	    return true;
	}
}

