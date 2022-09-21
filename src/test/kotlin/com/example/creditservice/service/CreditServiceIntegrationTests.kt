package com.example.creditservice.service

import com.example.creditservice.model.AddCreditOperation
import com.example.creditservice.model.Credit
import com.example.creditservice.model.UseCreditOperation
import com.example.creditservice.model.CreditUsageStatus
import com.example.creditservice.model.dto.AddCreditRequest
import org.h2.tools.Server
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.Date
import javax.persistence.EntityManager
import javax.sql.DataSource

@SpringBootTest
@Transactional
class CreditServiceIntegrationTests {

    @Autowired
    private lateinit var creditService: CreditService

    @Autowired
    private lateinit var dateFormatter: DateFormatter

    @Autowired
    protected lateinit var dataSource: DataSource

    @Autowired
    protected lateinit var entityManager: EntityManager

    fun openH2Console() {
        Server.startWebServer(DataSourceUtils.getConnection(dataSource))
    }

    @Test
    private fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }

    @Test
    fun `it should create a new credit with add credit operation when add credit request received`() {
        //given
        val request = AddCreditRequest(123L,"user1", BigDecimal.TEN, dateFormatter.format(Date()), dateFormatter.format(Date()))
        //when
        creditService.addCredit(request)
        //then
        flushAndClear()

        val creditOperation = entityManager.createQuery(
            "select c from AddCreditOperation c where c.credit.id = 123",AddCreditOperation::class.java).singleResult
        val credit = entityManager.find(Credit::class.java,123L)

        MatcherAssert.assertThat(creditOperation.amount,Matchers.equalTo(BigDecimal("10.00")))

        MatcherAssert.assertThat(credit.id,Matchers.equalTo(request.id))
        MatcherAssert.assertThat(credit.userId,Matchers.equalTo(request.userId))
        MatcherAssert.assertThat(credit.amount,Matchers.equalTo(BigDecimal("10.00")))
        MatcherAssert.assertThat(credit.startDate.time,Matchers.equalTo(dateFormatter.parse(request.startDate).time))
        MatcherAssert.assertThat(credit.expireDate!!.time,Matchers.equalTo(dateFormatter.parse(request.expireDate!!).time))

    }


    @Test
    fun `it should reserve given amount from available credits`() {
        //given
        creditService.addCredit(AddCreditRequest(123,"user1", BigDecimal.TEN,"2022-09-21 00:00:00", "2022-12-22 23:59:59"))
        creditService.addCredit(AddCreditRequest(456,"user1", BigDecimal.TEN,"2022-09-21 00:00:00", "2022-11-22 23:59:59"))
        creditService.addCredit(AddCreditRequest(789,"user1", BigDecimal.TEN,"2022-09-21 00:00:00", "2022-10-22 23:59:59"))
        creditService.addCredit(AddCreditRequest(987,"user1", BigDecimal.TEN,"2022-09-21 00:00:00", "2022-09-22 23:59:59"))
        //when
        flushAndClear()
        creditService.reserveCreditAmount("user1",BigDecimal("25"),"pat-123")
        //then
        flushAndClear()

        val credits = entityManager.createQuery("select c from Credit c order by c.expireDate asc",Credit::class.java).resultList

        MatcherAssert.assertThat(credits.get(0).amount,Matchers.equalTo(BigDecimal("0.00")))
        MatcherAssert.assertThat(credits.get(1).amount,Matchers.equalTo(BigDecimal("0.00")))
        MatcherAssert.assertThat(credits.get(2).amount,Matchers.equalTo(BigDecimal("5.00")))
        MatcherAssert.assertThat(credits.get(3).amount,Matchers.equalTo(BigDecimal("10.00")))

        MatcherAssert.assertThat(credits.get(0).creditOperations.size,Matchers.equalTo(2))
        MatcherAssert.assertThat(credits.get(1).creditOperations.size,Matchers.equalTo(2))
        MatcherAssert.assertThat(credits.get(2).creditOperations.size,Matchers.equalTo(2))
        MatcherAssert.assertThat(credits.get(3).creditOperations.size,Matchers.equalTo(1))

    }

    @Test
    fun `it should capture credits for the given payment attempt id`() {
        //given
        creditService.addCredit(AddCreditRequest(456,"user1", BigDecimal.TEN,"2022-09-21 00:00:00", "2022-11-22 23:59:59"))
        creditService.addCredit(AddCreditRequest(789,"user1", BigDecimal.TEN,"2022-09-21 00:00:00", "2022-10-22 23:59:59"))
        creditService.addCredit(AddCreditRequest(987,"user1", BigDecimal.TEN,"2022-09-21 00:00:00", "2022-09-22 23:59:59"))

        flushAndClear()
        val paymentAttemptId = "pat-123"
        creditService.reserveCreditAmount("user1",BigDecimal("25"),paymentAttemptId)
        //then
        flushAndClear()
        //when
        creditService.captureCredits(paymentAttemptId)

        flushAndClear()

        val useCreditOperations = entityManager.createQuery(
            "select c from UseCreditOperation c",UseCreditOperation::class.java).resultList

        useCreditOperations.forEach {
            MatcherAssert.assertThat(it.status,Matchers.equalTo(CreditUsageStatus.Captured))
        }
    }
    @Test
    fun `it should revert reserved credits for the given payment attempt id`() {
        //given
        creditService.addCredit(AddCreditRequest(456,"user1", BigDecimal.TEN,"2022-09-21 00:00:00", "2022-11-22 23:59:59"))
        creditService.addCredit(AddCreditRequest(789,"user1", BigDecimal.TEN,"2022-09-21 00:00:00", "2022-10-22 23:59:59"))
        creditService.addCredit(AddCreditRequest(987,"user1", BigDecimal.TEN,"2022-09-21 00:00:00", "2022-09-22 23:59:59"))

        flushAndClear()

        val paymentAttemptId = "pat-123"

        creditService.reserveCreditAmount("user1",BigDecimal("25"),paymentAttemptId)

        //then
        flushAndClear()

        //when
        creditService.revertCredits(paymentAttemptId)

        flushAndClear()

        val useCreditOperations = entityManager.createQuery("select c from UseCreditOperation c",UseCreditOperation::class.java).resultList

        useCreditOperations.forEach {
            MatcherAssert.assertThat(it.status,Matchers.equalTo(CreditUsageStatus.Reverted))
            MatcherAssert.assertThat(it.credit.amount,Matchers.equalTo(BigDecimal("10.00")))
        }
    }
}