package com.bistroops.reservation.model;

import java.time.LocalDateTime;

public interface ReservationAvailabilityRow {

    Long getRsvDtNo();

    LocalDateTime getDiningTime();

    Integer getTableType();

    Long getRemaining();
}