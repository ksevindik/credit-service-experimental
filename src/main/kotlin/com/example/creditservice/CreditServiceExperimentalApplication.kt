package com.example.creditservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.TimeZone

@SpringBootApplication
class CreditServiceExperimentalApplication {
	init {
	    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
	}
}

fun main(args: Array<String>) {
	runApplication<CreditServiceExperimentalApplication>(*args)
}
