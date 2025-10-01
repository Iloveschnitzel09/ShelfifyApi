package com.shelfify.shelfifyapi;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;


@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.shelfify.shelfifyapi")
@EnableAsync
@EnableJpaRepositories(basePackages = {
        "com.shelfify.shelfifyapi.repository",
        "com.shelfify.shelfifyapi.ean"
})
@EntityScan(basePackages = "com.shelfify.shelfifyapi")
public class ShelfifyApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(ShelfifyApiApplication.class, args);
	}

	@Bean
	public CommandLineRunner printAllEndpoints(ApplicationContext ctx) {
		return args -> {
			RequestMappingHandlerMapping mapping = ctx.getBean(RequestMappingHandlerMapping.class);
			mapping.getHandlerMethods().forEach((key, value) -> System.out.println(key+ ""+ value));
		};
	}
}
