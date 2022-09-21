package com.example.creditservice.service

import com.example.creditservice.model.Credit
import com.example.creditservice.model.UseCreditOperation
import com.example.creditservice.model.CreditUsageStatus
import com.example.creditservice.repository.CreditOperationRepository
import com.example.creditservice.repository.CreditRepository
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.util.Date

class CreditServiceRevertCreditUnitTests {
    @Test
    fun `it should revert reserved credit amounts for the given payment attempt id`() {
        //given
        val creditRepository = Mockito.mock(CreditRepository::class.java)
        val creditOperationRepository = Mockito.mock(CreditOperationRepository::class.java)
        val dateFormatter = DateFormatter()
        val timeFactory = Mockito.mock(TimeFactory::class.java)
        val creditService = CreditService(creditRepository,creditOperationRepository, dateFormatter, timeFactory)

        val userId = "user1"
        val paymentAttemptId = "pat-123"

        val credit2 = Credit(222,userId, BigDecimal("5.00"), Date(1000), Date(1003))
        val credit3 = Credit(333,userId, BigDecimal("0.00"), Date(1000), Date(1002))
        val credit4 = Credit(444,userId, BigDecimal("0.00"), Date(1000), Date(1001))

        val useCreditOperation4 = UseCreditOperation(credit4, BigDecimal("10.00"), paymentAttemptId)
        val useCreditOperation3 = UseCreditOperation(credit3, BigDecimal("10.00"), paymentAttemptId)
        val useCreditOperation2 = UseCreditOperation(credit2, BigDecimal("5.00"), paymentAttemptId)

        Mockito.doReturn(listOf(useCreditOperation2,useCreditOperation3,useCreditOperation4))
            .`when`(creditOperationRepository).findByPaymentAttemptId(paymentAttemptId)

        //when
        creditService.revertCredits(paymentAttemptId)

        //then

        MatcherAssert.assertThat(useCreditOperation2.status, Matchers.equalTo(CreditUsageStatus.Reverted))
        MatcherAssert.assertThat(useCreditOperation3.status, Matchers.equalTo(CreditUsageStatus.Reverted))
        MatcherAssert.assertThat(useCreditOperation4.status, Matchers.equalTo(CreditUsageStatus.Reverted))

        creditOperationRepository.save(useCreditOperation2)
        creditOperationRepository.save(useCreditOperation3)
        creditOperationRepository.save(useCreditOperation4)

        MatcherAssert.assertThat(credit2.amount, Matchers.equalTo(BigDecimal("10.00")))
        MatcherAssert.assertThat(credit3.amount, Matchers.equalTo(BigDecimal("10.00")))
        MatcherAssert.assertThat(credit4.amount, Matchers.equalTo(BigDecimal("10.00")))
    }
}