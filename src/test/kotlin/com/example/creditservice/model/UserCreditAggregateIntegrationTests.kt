package com.example.creditservice.model

import com.example.creditservice.BaseIntegrationTests
import com.example.creditservice.model.dto.AddCreditRequest
import com.example.creditservice.repository.CreditOperationRepository
import com.example.creditservice.repository.CreditRepository
import com.example.creditservice.service.DateFormatter
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.util.Date

class UserCreditAggregateIntegrationTests : BaseIntegrationTests() {

    @Autowired
    private lateinit var dateFormatter: DateFormatter

    @Autowired
    private lateinit var creditRepository: CreditRepository

    @Autowired
    private lateinit var creditOperationRepository: CreditOperationRepository

    @Test
    fun `it should create a new credit with add credit operation when add credit request received`() {
        //given
        val requestId = 123L
        val amount = BigDecimal.TEN
        val userId = "user1"
        val date = Date()
        val userCreditAggregate = UserCreditAggregate(userId,date.toInstant(),creditRepository,creditOperationRepository)
        //when
        userCreditAggregate.addCredit(requestId,amount,date,date)
        //then
        flushAndClear()

        val creditOperation = entityManager.createQuery(
            "select c from AddCreditOperation c where c.credit.id = 123",AddCreditOperation::class.java).singleResult
        val credit = entityManager.find(Credit::class.java,123L)

        MatcherAssert.assertThat(creditOperation.amount, Matchers.equalTo(BigDecimal("10.00")))

        MatcherAssert.assertThat(credit.id, Matchers.equalTo(requestId))
        MatcherAssert.assertThat(credit.userId, Matchers.equalTo(userId))
        MatcherAssert.assertThat(credit.amount, Matchers.equalTo(BigDecimal("10.00")))
        MatcherAssert.assertThat(credit.startDate.time, Matchers.equalTo(date.time))
        MatcherAssert.assertThat(credit.expireDate!!.time, Matchers.equalTo(date.time))

    }


    @Test
    fun `it should reserve given amount from available credits`() {
        //given
        val userId = "user1"
        val date = Date()
        val userCreditAggregate = UserCreditAggregate(userId,date.toInstant(),creditRepository,creditOperationRepository)

        userCreditAggregate.addCredit(123, BigDecimal.TEN,dateFormatter.parse("2022-09-21 00:00:00"), dateFormatter.parse("2022-12-22 23:59:59"))
        userCreditAggregate.addCredit(456, BigDecimal.TEN,dateFormatter.parse("2022-09-21 00:00:00"), dateFormatter.parse("2022-11-22 23:59:59"))
        userCreditAggregate.addCredit(789, BigDecimal.TEN,dateFormatter.parse("2022-09-21 00:00:00"), dateFormatter.parse("2022-10-22 23:59:59"))
        userCreditAggregate.addCredit(987, BigDecimal.TEN,dateFormatter.parse("2022-09-21 00:00:00"), dateFormatter.parse("2022-09-22 23:59:59"))
        //when
        flushAndClear()

        userCreditAggregate.reserveCreditAmount(BigDecimal("25"),"pat-123")
        //then
        flushAndClear()

        val credits = entityManager.createQuery("select c from Credit c order by c.expireDate asc",Credit::class.java).resultList

        MatcherAssert.assertThat(credits.get(0).amount, Matchers.equalTo(BigDecimal("0.00")))
        MatcherAssert.assertThat(credits.get(1).amount, Matchers.equalTo(BigDecimal("0.00")))
        MatcherAssert.assertThat(credits.get(2).amount, Matchers.equalTo(BigDecimal("5.00")))
        MatcherAssert.assertThat(credits.get(3).amount, Matchers.equalTo(BigDecimal("10.00")))

        MatcherAssert.assertThat(credits.get(0).creditOperations.size, Matchers.equalTo(2))
        MatcherAssert.assertThat(credits.get(1).creditOperations.size, Matchers.equalTo(2))
        MatcherAssert.assertThat(credits.get(2).creditOperations.size, Matchers.equalTo(2))
        MatcherAssert.assertThat(credits.get(3).creditOperations.size, Matchers.equalTo(1))

    }

    @Test
    fun `it should capture credits for the given payment attempt id`() {
        //given
        val userId = "user1"
        val date = Date()
        val userCreditAggregate = UserCreditAggregate(userId,date.toInstant(),creditRepository,creditOperationRepository)

        userCreditAggregate.addCredit(456, BigDecimal.TEN,dateFormatter.parse("2022-09-21 00:00:00"), dateFormatter.parse("2022-11-22 23:59:59"))
        userCreditAggregate.addCredit(789, BigDecimal.TEN,dateFormatter.parse("2022-09-21 00:00:00"), dateFormatter.parse("2022-10-22 23:59:59"))
        userCreditAggregate.addCredit(987, BigDecimal.TEN,dateFormatter.parse("2022-09-21 00:00:00"), dateFormatter.parse("2022-09-22 23:59:59"))

        flushAndClear()
        val paymentAttemptId = "pat-123"
        userCreditAggregate.reserveCreditAmount(BigDecimal("25"),paymentAttemptId)
        //then
        flushAndClear()
        //when
        userCreditAggregate.captureCredits(paymentAttemptId)

        flushAndClear()

        val useCreditOperations = entityManager.createQuery(
            "select c from UseCreditOperation c",UseCreditOperation::class.java).resultList

        useCreditOperations.forEach {
            MatcherAssert.assertThat(it.status, Matchers.equalTo(CreditUsageStatus.Captured))
        }
    }
    @Test
    fun `it should revert reserved credits for the given payment attempt id`() {
        //given
        val userId = "user1"
        val date = Date()
        val userCreditAggregate = UserCreditAggregate(userId,date.toInstant(),creditRepository,creditOperationRepository)

        userCreditAggregate.addCredit(456, BigDecimal.TEN,dateFormatter.parse("2022-09-21 00:00:00"), dateFormatter.parse("2022-11-22 23:59:59"))
        userCreditAggregate.addCredit(789, BigDecimal.TEN,dateFormatter.parse("2022-09-21 00:00:00"), dateFormatter.parse("2022-10-22 23:59:59"))
        userCreditAggregate.addCredit(987, BigDecimal.TEN,dateFormatter.parse("2022-09-21 00:00:00"), dateFormatter.parse("2022-09-22 23:59:59"))

        flushAndClear()

        val paymentAttemptId = "pat-123"

        userCreditAggregate.reserveCreditAmount(BigDecimal("25"),paymentAttemptId)

        //then
        flushAndClear()

        //when
        userCreditAggregate.revertCredits(paymentAttemptId)

        flushAndClear()

        val useCreditOperations = entityManager.createQuery("select c from UseCreditOperation c",UseCreditOperation::class.java).resultList

        useCreditOperations.forEach {
            MatcherAssert.assertThat(it.status, Matchers.equalTo(CreditUsageStatus.Reverted))
            MatcherAssert.assertThat(it.credit.amount, Matchers.equalTo(BigDecimal("10.00")))
        }
    }
}