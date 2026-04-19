package com.hackkosice.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HackKosiceServerApplication

fun main(args: Array<String>) {
    runApplication<HackKosiceServerApplication>(*args)
}
