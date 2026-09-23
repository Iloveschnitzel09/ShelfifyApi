package de.schnitzel.shelfifyapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ShelfifyApiApplication

fun main(args: Array<String>) {
    runApplication<ShelfifyApiApplication>(*args)
    println("Server started on port 8080")
}
