package com.bistroops.member.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<MemberVO, Integer> {

	MemberVO findByMemTel(String memTel);

	@Query("SELECT m FROM MemberVO m WHERE (:name = '' OR LOCATE(:name, m.memName) > 0) "
	        + "AND (:phone = '' OR m.memTel = :phone) ORDER BY m.memNo")
	java.util.List<MemberVO> searchForStaff(@Param("name") String name, @Param("phone") String phone);

	// 電話不存在時新增未驗證帳號。
	// 如果另一個請求已建立同電話帳號，就保留既有資料。
	@Modifying
	@Query(value = """
			INSERT INTO member (
			    mem_tel,
			    mem_password,
			    mem_status
			)
			VALUES (
			    :phone,
			    :encodedPassword,
			    '未驗證'
			)
			ON DUPLICATE KEY UPDATE
			    mem_no = member.mem_no
			""", nativeQuery = true)
	int insertUnverifiedMemberIfAbsent(@Param("phone") String phone, @Param("encodedPassword") String encodedPassword);
}
