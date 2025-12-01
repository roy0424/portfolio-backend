package dev.kyhan.portfolio

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PortfolioServiceApplication

fun main(args: Array<String>) {
    runApplication<PortfolioServiceApplication>(*args)
}
