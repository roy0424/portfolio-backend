package dev.kyhan.page

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PageServiceApplication

fun main(args: Array<String>) {
    runApplication<PageServiceApplication>(*args)
}