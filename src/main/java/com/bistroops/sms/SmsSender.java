package com.bistroops.sms;

/**
 * 簡訊傳送方式的共用介面。
 * 不依賴會員、訂位或候位功能。
 */
public interface SmsSender {

    void send(String phone, String message);
}