package com.lager.lagerappapi;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;


@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.lager.lagerappapi")
@EnableJpaRepositories(basePackages = {
		"com.lager.lagerappapi.repository",
		"com.lager.lagerappapi.ean"
})
@EntityScan(basePackages = "com.lager.lagerappapi")
public class LagerAppApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(LagerAppApiApplication.class, args);
	}

	@Bean
	public CommandLineRunner printAllEndpoints(ApplicationContext ctx) {
		return args -> {
			RequestMappingHandlerMapping mapping = ctx.getBean(RequestMappingHandlerMapping.class);
			mapping.getHandlerMethods().forEach((key, value) -> System.out.println(key+ ""+ value));
		};
	}
}
