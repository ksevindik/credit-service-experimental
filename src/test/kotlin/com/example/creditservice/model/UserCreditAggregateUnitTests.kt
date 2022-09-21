package com.example.creditservice.model

import com.example.creditservice.repository.CreditOperationRepository
import com.example.creditservice.repository.CreditRepository
import com.example.creditservice.service.DateFormatter
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.util.Date
import java.util.Optional

class UserCreditAggregateUnitTests {
    @Test
    fun `available credit amount should be zero when there are not any valid credits for given userId and instant`() {
        //given
        val creditRepository = Mockito.mock(CreditRepository::class.java)
        val creditOperationRepository = Mockito.mock(CreditOperationRepository::class.java)
        val dateFormatter = DateFormatter()
        val userId = "user1"
        val timePoint = dateFormatter.parse("2022-09-21 00:00:00")
        val userCreditAggregate = UserCreditAggregate(userId,timePoint.toInstant(),creditRepository, creditOperationRepository)
        Mockito.doReturn(emptyList<Credit>()).`when`(creditRepository).findAvailableCredits(userId, timePoint)
        //when
        val availableCreditAmount : BigDecimal = userCreditAggregate.getAvailableCreditAmount()
        //then
        MatcherAssert.assertThat(availableCreditAmount, Matchers.equalTo(BigDecimal.ZERO))
    }

    @Test
    fun `available credit amount should be sum of valid credits for given userId and instant`() {
        //given
        val creditRepository = Mockito.mock(CreditRepository::class.java)
        val creditOperationRepository = Mockito.mock(CreditOperationRepository::class.java)
        val dateFormatter = DateFormatter()
        val userId = "user1"
        val timePoint = dateFormatter.parse("2022-09-21 00:00:00").toInstant()
        val userCreditAggregate = UserCreditAggregate(userId,timePoint,creditRepository, creditOperationRepository)
        val credit1 = Credit(123,userId, BigDecimal("4.00"), Date())
        val credit2 = Credit(456,userId, BigDecimal("5.00"),Date())

        Mockito.doReturn(listOf(credit1,credit2)).`when`(creditRepository).findAvailableCredits(userId, Date.from(timePoint))
        //when
        val availableCreditAmount : BigDecimal = userCreditAggregate.getAvailableCreditAmount()
        //then
        MatcherAssert.assertThat(availableCreditAmount, Matchers.equalTo(BigDecimal("9.00")))
    }

    @Test
    fun `it should add a new credit for this user given the credit amount, start and expire dates`() {
        //given
        val creditRepository = StubCreditRepository()
        val creditOperationRepository = StubCreditOperationRepository()
        val dateFormatter = DateFormatter()
        val userId = "user1"
        val timePoint = dateFormatter.parse("2022-09-21 00:00:00")
        val userCreditAggregate = UserCreditAggregate(userId,timePoint.toInstant(),creditRepository, creditOperationRepository)
        val requestId = 123L
        val amount = BigDecimal("10.00")
        val startDate = timePoint
        val expireDate = dateFormatter.parse("2032-09-21 00:00:00")

        //when
        userCreditAggregate.addCredit(requestId,amount,startDate,expireDate)

        //then
        val addedCredit = creditRepository.savedEntity!!
        val addCreditOperation = creditOperationRepository.savedEntity!!

        MatcherAssert.assertThat(addedCredit.id,Matchers.equalTo(requestId))
        MatcherAssert.assertThat(addedCredit.userId,Matchers.equalTo(userId))
        MatcherAssert.assertThat(addedCredit.amount,Matchers.equalTo(amount))
        MatcherAssert.assertThat(addedCredit.startDate,Matchers.equalTo(startDate))
        MatcherAssert.assertThat(addedCredit.expireDate,Matchers.equalTo(expireDate))

        MatcherAssert.assertThat(addCreditOperation.javaClass,Matchers.equalTo(AddCreditOperation::class.java))
        MatcherAssert.assertThat(addCreditOperation.amount,Matchers.equalTo(amount))
        MatcherAssert.assertThat(addCreditOperation.credit,Matchers.sameInstance(addedCredit))
    }

    @Test
    fun `reserve credit should decrease the given amount from available credits in the order of oldest one first`() {
        //given
        val creditRepository = Mockito.mock(CreditRepository::class.java)
        val creditOperationRepository = Mockito.mock(CreditOperationRepository::class.java)
        val dateFormatter = DateFormatter()
        val userId = "user1"
        val timePoint = dateFormatter.parse("2022-09-21 00:00:00")
        val userCreditAggregate = UserCreditAggregate(userId,timePoint.toInstant(),creditRepository, creditOperationRepository)

        val credit1 = Credit(111,userId,BigDecimal(10), Date(1000), Date(1004))
        val credit2 = Credit(222,userId,BigDecimal(10), Date(1000), Date(1003))
        val credit3 = Credit(333,userId,BigDecimal(10), Date(1000), Date(1002))
        val credit4 = Credit(444,userId,BigDecimal(10), Date(1000), Date(1001))

        Mockito.doReturn(listOf(credit4,credit3,credit2,credit1)).`when`(creditRepository).findAvailableCredits(userId, timePoint)

        val reserveAmount = BigDecimal(25)
        val paymentAttemptId = "pat-123"

        //when
        userCreditAggregate.reserveCreditAmount(reserveAmount, paymentAttemptId)

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

    @Test
    fun `it should change status of credit usages to captured for the given payment attempt id`() {
        //given
        val creditRepository = Mockito.mock(CreditRepository::class.java)
        val creditOperationRepository = Mockito.mock(CreditOperationRepository::class.java)
        val dateFormatter = DateFormatter()
        val userId = "user1"
        val timePoint = dateFormatter.parse("2022-09-21 00:00:00").toInstant()
        val userCreditAggregate = UserCreditAggregate(userId,timePoint,creditRepository, creditOperationRepository)

        val paymentAttemptId = "pat-123"

        val credit2 = Credit(222,userId, BigDecimal("5.00"), Date(1000), Date(1003))
        val credit3 = Credit(333,userId, BigDecimal("0.00"), Date(1000), Date(1002))
        val credit4 = Credit(444,userId, BigDecimal("0.00"), Date(1000), Date(1001))

        val useCreditOperation4 = UseCreditOperation(credit4, BigDecimal("10.00"), paymentAttemptId)
        val useCreditOperation3 = UseCreditOperation(credit3,BigDecimal("10.00"), paymentAttemptId)
        val useCreditOperation2 = UseCreditOperation(credit2,BigDecimal("5.00"), paymentAttemptId)

        Mockito.doReturn(listOf(useCreditOperation2,useCreditOperation3,useCreditOperation4))
            .`when`(creditOperationRepository).findByPaymentAttemptId(paymentAttemptId)

        //when
        userCreditAggregate.captureCredits(paymentAttemptId)

        //then

        MatcherAssert.assertThat(useCreditOperation2.status,Matchers.equalTo(CreditUsageStatus.Captured))
        MatcherAssert.assertThat(useCreditOperation3.status,Matchers.equalTo(CreditUsageStatus.Captured))
        MatcherAssert.assertThat(useCreditOperation4.status,Matchers.equalTo(CreditUsageStatus.Captured))

        creditOperationRepository.save(useCreditOperation2)
        creditOperationRepository.save(useCreditOperation3)
        creditOperationRepository.save(useCreditOperation4)
    }

    @Test
    fun `it should revert reserved credit amounts for the given payment attempt id`() {
        //given
        val creditRepository = Mockito.mock(CreditRepository::class.java)
        val creditOperationRepository = Mockito.mock(CreditOperationRepository::class.java)
        val dateFormatter = DateFormatter()
        val userId = "user1"
        val timePoint = dateFormatter.parse("2022-09-21 00:00:00").toInstant()
        val userCreditAggregate = UserCreditAggregate(userId,timePoint,creditRepository, creditOperationRepository)

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
        userCreditAggregate.revertCredits(paymentAttemptId)

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

class StubCreditRepository : CreditRepository {
    var savedEntity : Credit? = null
    override fun findAvailableCredits(userId: String, expireDate:Date): List<Credit> {
        TODO("Not yet implemented")
    }

    override fun <S : Credit?> save(entity: S): S {
        savedEntity = entity
        return entity
    }

    override fun <S : Credit?> saveAll(entities: MutableIterable<S>): MutableList<S> {
        TODO("Not yet implemented")
    }

    override fun findAll(): MutableList<Credit> {
        TODO("Not yet implemented")
    }

    override fun findAllById(ids: MutableIterable<Long>): MutableList<Credit> {
        TODO("Not yet implemented")
    }

    override fun count(): Long {
        TODO("Not yet implemented")
    }

    override fun delete(entity: Credit) {
        TODO("Not yet implemented")
    }

    override fun deleteAllById(ids: MutableIterable<Long>) {
        TODO("Not yet implemented")
    }

    override fun deleteAll(entities: MutableIterable<Credit>) {
        TODO("Not yet implemented")
    }

    override fun deleteAll() {
        TODO("Not yet implemented")
    }

    override fun deleteById(id: Long) {
        TODO("Not yet implemented")
    }

    override fun existsById(id: Long): Boolean {
        TODO("Not yet implemented")
    }

    override fun findById(id: Long): Optional<Credit> {
        TODO("Not yet implemented")
    }

}

class StubCreditOperationRepository : CreditOperationRepository {
    var savedEntity : CreditOperation? = null
    override fun findByPaymentAttemptId(paymentAttemptId: String): List<UseCreditOperation> {
        TODO("Not yet implemented")
    }

    override fun <S : CreditOperation?> save(entity: S): S {
        savedEntity = entity
        return entity
    }

    override fun <S : CreditOperation?> saveAll(entities: MutableIterable<S>): MutableList<S> {
        TODO("Not yet implemented")
    }

    override fun findAll(): MutableList<CreditOperation> {
        TODO("Not yet implemented")
    }

    override fun findAllById(ids: MutableIterable<Long>): MutableList<CreditOperation> {
        TODO("Not yet implemented")
    }

    override fun count(): Long {
        TODO("Not yet implemented")
    }

    override fun delete(entity: CreditOperation) {
        TODO("Not yet implemented")
    }

    override fun deleteAllById(ids: MutableIterable<Long>) {
        TODO("Not yet implemented")
    }

    override fun deleteAll(entities: MutableIterable<CreditOperation>) {
        TODO("Not yet implemented")
    }

    override fun deleteAll() {
        TODO("Not yet implemented")
    }

    override fun deleteById(id: Long) {
        TODO("Not yet implemented")
    }

    override fun existsById(id: Long): Boolean {
        TODO("Not yet implemented")
    }

    override fun findById(id: Long): Optional<CreditOperation> {
        TODO("Not yet implemented")
    }

}