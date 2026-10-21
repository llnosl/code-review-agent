package com.codereview.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CodeReviewServerApplication {

	/**
	 * 启动服务端应用。
	 */
	public static void main(String[] args) {
		SpringApplication.run(CodeReviewServerApplication.class, args);
	}

}
