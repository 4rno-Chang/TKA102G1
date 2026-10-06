package com.bistroops.waiting.model;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface WaitingRepository extends JpaRepository<WaitingVO, Long> {

    List<WaitingVO> findAllByOrderByWaitingNoAsc();

    List<WaitingVO>
    findBySeatType_SeatTypeNoAndWaitingStatusInOrderByWaitingNoAsc(
            Integer seatTypeNo, Collection<String> statuses);

    List<WaitingVO> findByMember_MemNoOrderByWaitingNoDesc(Integer memNo);

    long countBySeatType_SeatTypeNoAndWaitingStatusIn(
            Integer seatTypeNo, Collection<String> statuses);

    long countBySeatType_SeatTypeNoAndWaitingNoLessThanAndWaitingStatusIn(
            Integer seatTypeNo, Long waitingNo, Collection<String> statuses);

    boolean existsBySeatType_SeatTypeNoAndWaitingTelAndWaitingStatusIn(
            Integer seatTypeNo, String waitingTel, Collection<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WaitingVO w WHERE w.waitingNo = :waitingNo")
    Optional<WaitingVO> findByIdForUpdate(@Param("waitingNo") Long waitingNo);

    // 以資料庫桌型資料作為取號鎖，避免同桌型、同電話同時重複送出。
    // nativeQuery 使用資料表欄位名稱；其餘方法依 Java 屬性查詢。
    @Query(value = """
        SELECT seat_type_no
        FROM seat_type
        WHERE seat_type_no = :seatTypeNo
        FOR UPDATE
        """, nativeQuery = true)
    Optional<Integer> lockSeatType(@Param("seatTypeNo") Integer seatTypeNo);
}
