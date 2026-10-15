package com.bistroops.orders.ecpay;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/* ========== 綠界金流小工具 ==========
   不用綠界的 Java SDK，自己做 SDK 會做的兩件事：
     1. checkMacValue：算簽章，讓綠界確認資料沒有被改過（送出時用、收到付款通知時也用）
     2. buildForm    ：產生一個會自動送出的 <form>，把瀏覽器帶到綠界的付款頁
   用在 OrdersController 的 payByCard（產生表單）和 ecpayReturn（驗證付款通知）。 
	商店資料改從 application.properties 讀，所以這裡要是 @Component（交給 Spring 管理），
     變數和方法都不能是 static（@Value 不能注入 static 變數），
     Controller 用 @Autowired 注入 EcpayUtil 來用。 */


@Component
public class EcpayUtil {
	
	 @Value("${ecpay.merchant-id}")
     private String merchantId;      // 商店代號

     @Value("${ecpay.hash-key}")
     private String hashKey;         // 算簽章用的金鑰，不能外流

     @Value("${ecpay.hash-iv}")
     private String hashIv;          // 算簽章用的金鑰，不能外流

     @Value("${ecpay.pay-url}")
     private String payUrl;          // 綠界付款網址（測試站）
     
     public String getMerchantId() {
         return merchantId;
 }


	// ---------- 1. 算 CheckMacValue（簽章） ----------
	// 步驟：參數照名稱的字母排序 → 前面加 HashKey、後面加 HashIV → URL encode → 轉小寫 → SHA256 → 轉大寫
	// 綠界收到後會用一樣的方法再算一次，兩邊算出來不一樣就代表資料被改過
	public String checkMacValue(Map<String, String> params) {
		Map<String, String> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER); // TreeMap 會自動照 key 排序，不分大小寫
		sorted.putAll(params);
		sorted.remove("CheckMacValue"); // 簽章本身不算在裡面（驗證付款通知時，綠界送來的參數會帶著它）

		// 組成 HashKey=xxx&參數1=值1&參數2=值2...&HashIV=yyy
		StringBuilder sb = new StringBuilder("HashKey=" + hashKey);
		sorted.forEach((k, v) -> sb.append('&').append(k).append('=').append(v));
		sb.append("&HashIV=").append(hashIv);

		// Java 的 URLEncoder 會把 ! ( ) 編成 %21 %28 %29，綠界（.NET）不會，所以換回來，兩邊才算得一樣
		String encoded = URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8).toLowerCase()
				.replace("%21", "!").replace("%28", "(").replace("%29", ")");
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(encoded.getBytes(StandardCharsets.UTF_8)); // SHA256 雜湊
			return HexFormat.of().withUpperCase().formatHex(hash); // 轉成大寫的 16 進位文字
		} catch (Exception e) {
			throw new RuntimeException(e); // SHA-256 是 Java 內建的，正常不會跑到這裡
		}
	}

	// ---------- 2. 產生會自動送出的表單 ----------
	// 回傳一段 HTML：所有參數放成隱藏欄位，最後一行 script 讓瀏覽器一收到就自動送到綠界
	// Controller 用 @ResponseBody 回傳這段文字，結果會顯示在 ecpayWin 小視窗裡
	public String buildForm(Map<String, String> params) {
		params.put("CheckMacValue", checkMacValue(params)); // 先算好簽章，跟著其他參數一起送出
		StringBuilder html = new StringBuilder("<form id='ecpay' method='post' action='" + payUrl + "'>");
		params.forEach((k, v) -> html.append("<input type='hidden' name='").append(k)
				.append("' value='").append(HtmlUtils.htmlEscape(v)).append("'>")); // htmlEscape：值裡有 ' < > 這些符號也不會弄壞 HTML
		html.append("</form><script>document.getElementById('ecpay').submit();</script>");
		return html.toString();
	}
}
