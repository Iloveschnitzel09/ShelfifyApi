package de.schnitzel.shelfifyapi

import org.springframework.beans.factory.getBean
import org.springframework.boot.CommandLineRunner
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@EnableScheduling
@EnableAsync
@SpringBootApplication
@EnableJpaRepositories
@EntityScan
class ShelfifyApiApplication {

    @Bean
    fun printAllEndpoints(ctx: ApplicationContext) = CommandLineRunner {
        val mapping = ctx.getBean<RequestMappingHandlerMapping>()
        mapping.handlerMethods.forEach { (key, value) ->
            println("$key $value")
        }
    }
}

fun main(args: Array<String>) {
    runApplication<ShelfifyApiApplication>(*args)
}
