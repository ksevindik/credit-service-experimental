package com.example.creditservice.service

import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

@Component
class DateFormatter(val pattern:String = "yyyy-MM-dd HH:mm:ss", val tzId:String = "UTC") {

    fun parse(value:String) : Date {
        val dateFormat = SimpleDateFormat(pattern)
        dateFormat.timeZone = TimeZone.getTimeZone(tzId)
        return dateFormat.parse(value)
    }

    fun format(value:Date) : String  {
        val dateFormat = SimpleDateFormat(pattern)
        dateFormat.timeZone = TimeZone.getTimeZone(tzId)
        return dateFormat.format(value)
    }
}