package com.example.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class TestApplication {

	public static void main(String[] args) {
		System.out.println("This is ShServer constructor");
		ApplicationContext context = SpringApplication.run(TestApplication.class, args);

		ShServer shServer = context.getBean(ShServer.class);
	}

}
