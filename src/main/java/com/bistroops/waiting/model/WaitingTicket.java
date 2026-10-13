package com.bistroops.waiting.model;

import java.util.Locale;

// Redis 候位資料，不是 JPA Entity，不寫入 MySQL。
public record WaitingTicket(String ticketId, String date, int tableType, long sequence, String phone, String name,
		String note, String status, long takenAt, long notifiedAt, long notifyDeadline, long expiresAt,
		String accessToken) {

	public String getTicketDisplay() {
		return tableType + "-" + String.format(Locale.ROOT, "%03d", sequence);
	}

	public String getStatusText() {
		return switch (status) {
		case "WAITING" -> "候位中";
		case "NOTIFIED" -> "已通知";
		case "SEATED" -> "已入座";
		case "MISSED" -> "過號";
		case "CANCELLED" -> "取消";
		default -> "未知狀態";
		};
	}

	// 避免直接輸出物件時印出電話或操作憑證。
	@Override
	public String toString() {
		return "WaitingTicket[date=" + date + ", number=" + getTicketDisplay() + ", status=" + status + "]";
	}
}