package com.example.creditservice.service

import org.springframework.stereotype.Component
import java.util.Date

@Component
class TimeFactory {
    fun getCurrentTime() : Date {
        return Date()
    }
}