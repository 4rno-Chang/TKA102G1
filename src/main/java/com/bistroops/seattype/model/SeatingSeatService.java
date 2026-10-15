package com.bistroops.seattype.model;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bistroops.seat.model.SeatVO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

/*
 * 桌位的讀寫（seat 資料表）
 * · 只注入自己的 SeatingSeatRepository；組員要知道哪些桌子有客人（例如點餐）可以注入這個 Service，
 *   用 getInUseSeats()、isInUse()。不要注入 SeatTypeService（避免互相依賴）。
 * · 會改桌位的方法都先用 PESSIMISTIC_WRITE 鎖住那一桌，兩個人同時按入座時，第二個人會看到「這桌已經有人」。
 */
@Service
@Transactional(readOnly = true)
public class SeatingSeatService {

	public static final String FREE = "可使用";
	public static final String RESERVED = "訂位者使用";
	public static final String WALK_IN = "其它者使用";
	public static final String OFF = "停用";

	private final SeatingSeatRepository repository;

	@PersistenceContext
	private EntityManager entityManager;

	public SeatingSeatService(SeatingSeatRepository repository) {
		this.repository = repository;
	}

	public List<SeatVO> getAll() {
		return repository.findAll(Sort.by("seatNo"));
	}

	// 有客人在坐的桌子（訂位者使用、其它者使用），給點餐的桌號下拉選單用
	public List<SeatVO> getInUseSeats() {
		return getAll().stream().filter(s -> isInUse(s)).toList();
	}

	// 送出訂單前再檢查一次：這桌現在有沒有客人
	public boolean isInUse(String seatNo) {
		return seatNo != null && repository.findById(seatNo).map(SeatingSeatService::isInUse).orElse(false);
	}

	public static boolean isInUse(SeatVO seat) {
		return RESERVED.equals(seat.getSeatStatus()) || WALK_IN.equals(seat.getSeatStatus());
	}

	// 入座：空桌 → 訂位者使用／其它者使用，入座時間記成現在
	@Transactional
	public SeatVO seatIn(String seatNo, String status) {
		if (!RESERVED.equals(status) && !WALK_IN.equals(status)) {
			throw new IllegalArgumentException("入座狀態錯誤");
		}
		SeatVO seat = lock(seatNo);
		if (!FREE.equals(seat.getSeatStatus())) {
			throw new IllegalStateException(seatNo + " 目前是「" + seat.getSeatStatus() + "」，不能入座（可能別人剛安排了，請重新整理）");
		}
		seat.setSeatStatus(status);
		seat.setSeatTime(LocalTime.now(java.time.ZoneId.of("Asia/Taipei")).truncatedTo(ChronoUnit.SECONDS));
		return seat;
	}

	// 客人離席：使用中 → 可使用
	@Transactional
	public SeatVO leave(String seatNo) {
		SeatVO seat = lock(seatNo);
		if (!isInUse(seat)) {
			throw new IllegalStateException(seatNo + " 目前沒有客人");
		}
		return clear(seat);
	}

	// 結帳後自動清桌、打烊清桌用：使用中的桌子清回可使用（已經不是使用中就不動）
	@Transactional
	public SeatVO release(String seatNo) {
		SeatVO seat = lock(seatNo);
		return isInUse(seat) ? clear(seat) : seat;
	}

	@Transactional
	public SeatVO disable(String seatNo) {
		SeatVO seat = lock(seatNo);
		if (!FREE.equals(seat.getSeatStatus())) {
			throw new IllegalStateException("只有空桌可以停用，請先讓客人離席");
		}
		seat.setSeatStatus(OFF);
		seat.setSeatTime(null);
		return seat;
	}

	@Transactional
	public SeatVO enable(String seatNo) {
		SeatVO seat = lock(seatNo);
		if (!OFF.equals(seat.getSeatStatus())) {
			throw new IllegalStateException(seatNo + " 沒有停用");
		}
		seat.setSeatStatus(FREE);
		return seat;
	}

	private SeatVO clear(SeatVO seat) {
		seat.setSeatStatus(FREE);
		seat.setSeatTime(null);
		return seat;
	}

	private SeatVO lock(String seatNo) {
		if (seatNo == null || seatNo.isBlank()) {
			throw new IllegalArgumentException("請選擇桌位");
		}
		SeatVO seat = entityManager.find(SeatVO.class, seatNo, LockModeType.PESSIMISTIC_WRITE);
		if (seat == null) {
			throw new IllegalArgumentException("找不到桌位 " + seatNo);
		}
		return seat;
	}

	// 撰寫by陳巧馨，查一桌+線上點餐要用入座時間 seat_time 當入座客人的代號

	public SeatVO getSeat(String seatNo) {
		if (seatNo == null)
			return null;
		return repository.findById(seatNo).orElse(null); // 查不到回傳 null
	}
}
