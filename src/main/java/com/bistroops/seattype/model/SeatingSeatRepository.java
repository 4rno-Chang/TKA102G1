package com.bistroops.seattype.model;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bistroops.seat.model.SeatVO;

/*
 * 入座管理用的桌位 Repository（SeatVO 是 li 的，這個 Repository 是入座管理自己的）
 * 只用 JpaRepository 內建的 findAll(Sort)、findById，不寫自訂查詢：
 * SeatVO 的屬性改名時，才不會讓整個專案啟動失敗。
 */
public interface SeatingSeatRepository extends JpaRepository<SeatVO, String> {
}
