package com.bistroops.member.model;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<MemberVO, Integer> {
	
	MemberVO findByMemTel(String memTel);

}