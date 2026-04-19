package com.hackkosice.server.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleBusinessErrors(ex: RuntimeException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to (ex.message ?: "Bad request")))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, String>> {
        val message = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Validation error"
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to message))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnknown(ex: Exception): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("error" to (ex.message ?: "Internal server error")))
    }
}
