package com.bistroops.member.model;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

	@Autowired
	MemberRepository dao;

	@Autowired
	private PasswordEncoder passwordEncoder;

	// 查詢全部會員
	public List<MemberVO> getAll() {
		return dao.findAll();
	}

	// 依會員編號查詢
	public MemberVO getMemNoQuery(Integer memNo) {
		return dao.findById(memNo).orElse(null);
	}

	// 註冊成會員 用手機號碼註冊 所以只寫上手機號碼及密碼
	public boolean insertMember(String memTel, String memPassword) {

		// 先檢查手機號碼是否已註冊
		if (isTelRegistered(memTel)) {

			// 手機號碼已註冊，返回 false
			return false;
		}

		// 尚未註冊，建立新會員
		MemberVO member = new MemberVO();

		member.setMemTel(memTel);
		member.setMemPassword(passwordEncoder.encode(memPassword));

		// 新註冊會員預設為未驗證
		member.setMemStatus("未驗證");

		dao.save(member);

		// 註冊成功，返回 true
		return true;
	}

	// 查詢此手機是否已註冊
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

	// 會員修改個人資料
	public boolean updateMember(Integer memNo, String memName, String memMail, String memBarcode, String memTag,
			LocalDate memBirth) {

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

	// 會員登入
	public MemberVO login(String memTel, String memPassword) {

		// 依手機號碼查詢會員資料
		MemberVO member = dao.findByMemTel(memTel);
		System.out.println("會員是否存在：" + (member != null));
		if (member != null) {
		    System.out.println("會員驗證狀態：" + member.getMemStatus());
		    System.out.println("是否為預設密碼：" +
		            passwordEncoder.matches("00000000", member.getMemPassword()));
		    System.out.println("輸入密碼是否正確：" +
		            passwordEncoder.matches(memPassword, member.getMemPassword()));
		}

		// 防呆：若會員資料不存在，則登入失敗
		if (member == null) {

			// 找不到會員
			return null;
		}

		String storedPassword = member.getMemPassword();

		if (storedPassword == null || storedPassword.isBlank()) {
			return null;
		}

		// 已驗證帳號不能再用公開預設密碼登入。
		// 電話驗證成功的使用者，應在當次會員中心設定個人密碼。
		if ("已驗證".equals(member.getMemStatus()) && passwordEncoder.matches("00000000", storedPassword)) {

			return null;
		}

		// 驗證密碼是否正確
		if (passwordEncoder.matches(memPassword, member.getMemPassword())) {

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
		member.setMemPassword(passwordEncoder.encode(newPassword));

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
		member.setMemPassword(passwordEncoder.encode(newPassword));

		// 儲存修改
		dao.save(member);

		return true;
	}

	// 會員手機驗證成功，更新驗證狀態
	public boolean verifyMemberPhone(Integer memNo) {

		// 依會員編號查詢資料庫
		MemberVO member = dao.findById(memNo).orElse(null);

		// 找不到會員
		if (member == null) {
			return false;
		}

		// 更新為已驗證
		member.setMemStatus("已驗證");

		// 儲存至 MySQL
		dao.save(member);

		return true;
	}

	// 訂位使用：依電話取得或建立未驗證會員。
	// 不會登入會員，也不會修改既有會員資料。
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public MemberVO getOrCreateUnverifiedMemberForReservation(String phone) {

		String normalizedPhone = phone == null ? "" : phone.trim();

		if (!normalizedPhone.matches("09[0-9]{8}")) {
			throw new IllegalArgumentException("請輸入09開頭的10碼手機號碼");
		}

		// 已有帳號，直接沿用。
		MemberVO existing = dao.findByMemTel(normalizedPhone);

		if (existing != null) {
			return existing;
		}

		// 預設密碼以現有 PasswordEncoder 編碼。
		String encodedPassword = passwordEncoder.encode("00000000");

		// 唯一索引處理同電話同時建立的情況。
		dao.insertUnverifiedMemberIfAbsent(normalizedPhone, encodedPassword);

		// 新建或其他請求先建立，都以資料庫中的帳號為準。
		MemberVO member = dao.findByMemTel(normalizedPhone);

		if (member == null) {
			throw new IllegalStateException("帳號建立結果無法確認，請稍後再試");
		}

		return member;
	}

	// 是否為仍使用預設密碼的未驗證會員
	public boolean hasUnverifiedDefaultPassword(String phone) {
		if (phone == null) {
			return false;
		}

		String normalizedPhone = phone.trim();

		if (!normalizedPhone.matches("09[0-9]{8}")) {
			return false;
		}

		MemberVO member = dao.findByMemTel(normalizedPhone);

		if (member == null || !"未驗證".equals(member.getMemStatus()) || member.getMemPassword() == null
				|| member.getMemPassword().isBlank()) {
			return false;
		}

		return passwordEncoder.matches("00000000", member.getMemPassword());
	}
}
