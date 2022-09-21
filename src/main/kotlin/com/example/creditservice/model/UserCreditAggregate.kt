package com.example.creditservice.model

import com.example.creditservice.repository.CreditRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.Date

class UserCreditAggregate(private val userId:String, private val instant:Instant, private val creditRepository: CreditRepository) {

    fun getAvailableCreditAmount(): BigDecimal {
        val availableCredits = creditRepository.findAvailableCredits(userId, Date.from(instant))
        return availableCredits.sumOf { it.amount }
    }
}