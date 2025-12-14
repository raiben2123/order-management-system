package com.ruben.ruben;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.ruben.ruben")
public class RubenApplication {

	public static void main(String[] args) {
		SpringApplication.run(RubenApplication.class, args);
	}

}
