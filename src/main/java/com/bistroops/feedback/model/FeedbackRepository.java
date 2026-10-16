package com.bistroops.feedback.model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackRepository extends JpaRepository<FeedbackVO, Integer> {

    // 共用的查詢條件
    String WHERE = """
                    WHERE (:orders IS NULL OR f.orders.orders = :orders)
                      AND (:tel    IS NULL OR f.feeCustomerTel = :tel)
                      AND (:start  IS NULL OR f.feeTime >= :start)
                      AND (:end    IS NULL OR f.feeTime <  :end)
                      AND (:rating IS NULL OR f.feeRating = :rating)
                    """;

    @Query("SELECT COUNT(f) FROM FeedbackVO f " + WHERE)
    long countByCondition(@Param("orders") Integer orders,
                          @Param("tel") String tel,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end,
                          @Param("rating") Integer rating);

    @Query("SELECT f FROM FeedbackVO f " + WHERE + " ORDER BY f.feeTime DESC")
    List<FeedbackVO> searchByCondition(@Param("orders") Integer orders,
                                       @Param("tel") String tel,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end,
                                       @Param("rating") Integer rating,
                                       Pageable pageable);

    @Query("SELECT AVG(f.feeRating) FROM FeedbackVO f " + WHERE)
    Double avgByCondition(@Param("orders") Integer orders,
                          @Param("tel") String tel,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end,
                          @Param("rating") Integer rating);
}
