package com.bistroops.announcement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication  // 注意: 當使用 maven install 匯出 jar 檔時，整個系統只能保留一個 @SpringBootApplication 的設定
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
