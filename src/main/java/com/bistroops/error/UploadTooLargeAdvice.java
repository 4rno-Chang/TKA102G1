package com.bistroops.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

/*
 * 上傳的檔案太大（超過 spring.servlet.multipart.max-file-size，預設 1MB）時顯示 413 錯誤頁
 *
 * 為什麼要另外處理：其他錯誤是 Spring 轉到 /error，再顯示 templates/error/{狀態碼}.html；
 * 但上傳太大時，轉到 /error 還是同一個上傳請求，Spring 會再解析一次檔案、再失敗一次，
 * 錯誤頁來不及產生，瀏覽器只會看到自己的「HTTP ERROR 413」。所以這裡直接回傳 error/413，不轉到 /error。
 *
 * 作用在全站的上傳（公告、活動、員工照片）：
 *   瀏覽器表單送出的（Accept 有 text/html）→ 顯示 error/413.html（「回上一頁」回到表單）
 *   其他（例如 JS 用 fetch 呼叫的 API）     → 跟原本一樣回 413、沒有內容，JS 的判斷不受影響
 * 檔案超過 2MB 時，Tomcat 會直接中斷連線（server.tomcat.max-swallow-size），不會進到這裡。
 */
@ControllerAdvice
public class UploadTooLargeAdvice {

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public Object uploadTooLarge(HttpServletRequest request) {
		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("text/html")) {
			ModelAndView errorPage = new ModelAndView("error/413", HttpStatus.PAYLOAD_TOO_LARGE);
			errorPage.addObject("status", HttpStatus.PAYLOAD_TOO_LARGE.value());
			errorPage.addObject("path", request.getRequestURI());   // error_layout 用來判斷前台／後台
			return errorPage;
		}
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
	}
}
