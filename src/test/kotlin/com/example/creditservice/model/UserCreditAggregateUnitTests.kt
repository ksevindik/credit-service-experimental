package com.example.creditservice.model

import com.example.creditservice.repository.CreditRepository
import com.example.creditservice.service.DateFormatter
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.util.Date

class UserCreditAggregateUnitTests {
    @Test
    fun `available credit amount should be zero when there are not any valid credits for given userId and instant`() {
        //given
        val creditRepository = Mockito.mock(CreditRepository::class.java)
        val dateFormatter = DateFormatter()
        val userId = "user1"
        val timePoint = dateFormatter.parse("2022-09-21 00:00:00").toInstant()
        val userCreditAggregate = UserCreditAggregate(userId,timePoint,creditRepository)
        Mockito.doReturn(emptyList<Credit>()).`when`(creditRepository).findAvailableCredits(userId, Date.from(timePoint))
        //when
        val availableCreditAmount : BigDecimal = userCreditAggregate.getAvailableCreditAmount()
        //then
        MatcherAssert.assertThat(availableCreditAmount, Matchers.equalTo(BigDecimal.ZERO))
    }

    @Test
    fun `available credit amount should be sum of valid credits for given userId and instant`() {
        //given
        val creditRepository = Mockito.mock(CreditRepository::class.java)
        val dateFormatter = DateFormatter()
        val userId = "user1"
        val timePoint = dateFormatter.parse("2022-09-21 00:00:00").toInstant()
        val userCreditAggregate = UserCreditAggregate(userId,timePoint,creditRepository)
        val credit1 = Credit(123,userId, BigDecimal("4.00"), Date())
        val credit2 = Credit(456,userId, BigDecimal("5.00"),Date())

        Mockito.doReturn(listOf(credit1,credit2)).`when`(creditRepository).findAvailableCredits(userId, Date.from(timePoint))
        //when
        val availableCreditAmount : BigDecimal = userCreditAggregate.getAvailableCreditAmount()
        //then
        MatcherAssert.assertThat(availableCreditAmount, Matchers.equalTo(BigDecimal("9.00")))
    }
}