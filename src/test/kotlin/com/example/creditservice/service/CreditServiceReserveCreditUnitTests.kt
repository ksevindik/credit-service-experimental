package com.example.creditservice.service

import com.example.creditservice.model.Credit
import com.example.creditservice.model.UseCreditOperation
import com.example.creditservice.repository.CreditOperationRepository
import com.example.creditservice.repository.CreditRepository
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.util.Date

class CreditServiceReserveCreditUnitTests {
    @Test
    fun `reserve credit should decrease the given amount from available credits in the order of oldest one first`() {
        //given
        val reserveAmount = BigDecimal(25)
        val userId = "user1"
        val creditRepository = Mockito.mock(CreditRepository::class.java)
        val creditOperationRepository = Mockito.mock(CreditOperationRepository::class.java)
        val dateFormatter = DateFormatter()
        val timeFactory = Mockito.mock(TimeFactory::class.java)
        val creditService = CreditService(creditRepository,creditOperationRepository, dateFormatter, timeFactory)

        Mockito.doReturn(Date(1000)).`when`(timeFactory).getCurrentTime()

        val credit1 = Credit(111,userId,BigDecimal(10), Date(1000), Date(1004))
        val credit2 = Credit(222,userId,BigDecimal(10), Date(1000), Date(1003))
        val credit3 = Credit(333,userId,BigDecimal(10), Date(1000), Date(1002))
        val credit4 = Credit(444,userId,BigDecimal(10), Date(1000), Date(1001))

        Mockito.doReturn(listOf(credit4,credit3,credit2,credit1)).`when`(creditRepository).findAvailableCredits(userId, Date(1000))

        val paymentAttemptId = "pat-123"

        //when
        creditService.reserveCreditAmount(userId,reserveAmount, paymentAttemptId)
        //then

        MatcherAssert.assertThat(credit4.amount, Matchers.equalTo(BigDecimal.ZERO))
        MatcherAssert.assertThat(credit3.amount, Matchers.equalTo(BigDecimal.ZERO))
        MatcherAssert.assertThat(credit2.amount, Matchers.equalTo(BigDecimal.valueOf(5)))
        MatcherAssert.assertThat(credit1.amount, Matchers.equalTo(BigDecimal.TEN))


        val useCreditOperation4 = UseCreditOperation(credit4, BigDecimal("10.00"), paymentAttemptId)
        val useCreditOperation3 = UseCreditOperation(credit3,BigDecimal("10.00"), paymentAttemptId)
        val useCreditOperation2 = UseCreditOperation(credit2,BigDecimal("5.00"), paymentAttemptId)

        Mockito.verify(creditOperationRepository).save(useCreditOperation4)
        Mockito.verify(creditOperationRepository).save(useCreditOperation3)
        Mockito.verify(creditOperationRepository).save(useCreditOperation2)

        Mockito.verify(creditRepository).save(credit4)
        Mockito.verify(creditRepository).save(credit3)
        Mockito.verify(creditRepository).save(credit2)
        Mockito.verify(creditRepository,Mockito.never()).save(credit1)

    }
}