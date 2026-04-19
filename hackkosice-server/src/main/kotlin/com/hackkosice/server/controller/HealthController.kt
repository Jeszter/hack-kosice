package com.hackkosice.server.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")
class HealthController {
    @GetMapping
    fun root(): Map<String, String> {
        return mapOf("status" to "ok", "service" to "hackkosice-server")
    }
}
