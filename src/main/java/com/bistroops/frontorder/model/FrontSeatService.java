package com.bistroops.frontorder.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bistroops.orders.model.OrdersService;
import com.bistroops.seat.model.SeatVO;
import com.bistroops.seattype.model.SeatingSeatService;

@Service
public class FrontSeatService {
	
	//測試先改小，正式版為90分鐘
	public static final int DINING_MINUTES = 10;
	
	public static final String MSG_CHANGED = "請重新掃描桌上的 QR code。";
	
	@Autowired
	private SeatingSeatService seatingSeatService;
	@Autowired 
	private OrdersService ordersService; 

	public SeatVO getSeat(String seatNo) {
		return seatingSeatService.getSeat(seatNo);
	}
	
	public String getSeatingKey(SeatVO seat) {
		return seat.getSeatNo() + ":" + seat.getSeatTime().format(DateTimeFormatter.ofPattern("HHmmss"));
	}
	
	//確認此掃描到的桌號客人是否能點餐
	public String checkSeat(SeatVO seat, String seatingKeyInSession) {
	    if (seat == null) {
	        return "查無此桌號，請洽服務人員。";
	    }

	    if (!SeatingSeatService.isInUse(seat) || seat.getSeatTime() == null) {
	        return "這一桌目前沒有開放點餐，請洽服務人員。";
	    }

	    //換了一組客人：這支手機記的代號，跟這一桌現在的代號不一樣
	    if (seatingKeyInSession != null && !seatingKeyInSession.equals(getSeatingKey(seat))) {
	        return "請重新掃描桌上的 QR code。";
	    }

	    LocalDateTime seatedAt = LocalDate.now().atTime(seat.getSeatTime());
	    if (LocalDateTime.now().isAfter(seatedAt.plusMinutes(DINING_MINUTES))) {
	        return "用餐時間已經結束，如需加點請洽服務人員。";
	    }

	    if (ordersService.isSeatingPaid(seat.getSeatNo(), seatedAt)) {
	        return "這一桌已經結帳，如需加點請洽服務人員。";
	    }

	    return null;   
	}

	//這一組客人「還沒結帳」的那張訂單的編號
	public Integer getUnpaidOrderNo(SeatVO seat) {
		LocalDateTime since = LocalDate.now().atTime(seat.getSeatTime());
		return ordersService.getUnpaidOrderNo(seat.getSeatNo(), since);
	}
}
