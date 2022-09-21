package com.example.creditservice.service

import com.example.creditservice.model.AddCreditOperation
import com.example.creditservice.model.Credit
import com.example.creditservice.model.CreditOperation
import com.example.creditservice.model.UseCreditOperation
import com.example.creditservice.model.dto.AddCreditRequest
import com.example.creditservice.repository.CreditOperationRepository
import com.example.creditservice.repository.CreditRepository
import com.nhaarman.mockitokotlin2.argumentCaptor
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Date
import java.util.Optional

class CreditServiceUnitTests {
    @Test
    fun `it should create a new credit with add credit operation when add credit request received (stub version)`() {
        //given
        val creditRepository = StubCreditRepository()
        val creditOperationRepository = StubCreditOperationRepository()
        val dateFormatter = DateFormatter()
        val timeFactory = TimeFactory()
        val creditService = CreditService(creditRepository,creditOperationRepository, dateFormatter, timeFactory)

        val addCreditRequest = AddCreditRequest(
            123L,
            "user1",
            BigDecimal(BigInteger.TEN),
            "2022-09-20 00:00:00",
            "2023-09-19 23:59:59")

        //when
        creditService.addCredit(addCreditRequest)

        //then
        val savedCredit = creditRepository.savedEntity!!
        val savedCreditOperation = creditOperationRepository.savedEntity!!
        MatcherAssert.assertThat(savedCreditOperation.credit,Matchers.sameInstance(savedCredit))
        MatcherAssert.assertThat(savedCreditOperation.javaClass,Matchers.equalTo(AddCreditOperation::class.java))
    }

    @Test
    fun `it should create a new credit with add credit operation when add credit request received (mock version)`() {
        //given
        val creditRepository = Mockito.mock(CreditRepository::class.java)
        val creditOperationRepository = Mockito.mock(CreditOperationRepository::class.java)
        val dateFormatter = DateFormatter()
        val timeFactory = TimeFactory()
        val creditService = CreditService(creditRepository,creditOperationRepository, dateFormatter, timeFactory)

        val addCreditRequest = AddCreditRequest(
            123L,
            "user1",
            BigDecimal(BigInteger.TEN),
            "2022-09-20 00:00:00",
            "2023-09-19 23:59:59")

        val mockCredit = Mockito.mock(Credit::class.java)
        Mockito.doReturn(addCreditRequest.id).`when`(mockCredit).id
        Mockito.doReturn(addCreditRequest.userId).`when`(mockCredit).userId
        Mockito.doReturn(addCreditRequest.amount).`when`(mockCredit).amount
        Mockito.doReturn(dateFormatter.parse(addCreditRequest.startDate)).`when`(mockCredit).startDate
        Mockito.doReturn(dateFormatter.parse(addCreditRequest.expireDate!!)).`when`(mockCredit).expireDate
        val creditCaptor = argumentCaptor<Credit>()
        Mockito.doReturn(mockCredit).`when`(creditRepository).save(creditCaptor.capture())

        val mockCreditOperation = Mockito.mock(CreditOperation::class.java)
        val creditOperationCaptor = argumentCaptor<CreditOperation>()
        Mockito.doReturn(mockCreditOperation).`when`(creditOperationRepository).save(creditOperationCaptor.capture())

        //when
        creditService.addCredit(addCreditRequest)

        //then
        val savedCredit = creditCaptor.firstValue
        val savedCreditOperation = creditOperationCaptor.firstValue
        MatcherAssert.assertThat(savedCreditOperation.credit,Matchers.sameInstance(mockCredit))
        MatcherAssert.assertThat(savedCreditOperation.javaClass,Matchers.equalTo(AddCreditOperation::class.java))
    }

    @Test
    fun `it should create a new credit with add credit operation when add credit request received (mock version 2)`() {
        //given
        val creditRepository = Mockito.mock(CreditRepository::class.java)
        val creditOperationRepository = Mockito.mock(CreditOperationRepository::class.java)
        val dateFormatter = DateFormatter()
        val timeFactory = TimeFactory()
        val creditService = CreditService(creditRepository,creditOperationRepository, dateFormatter, timeFactory)

        val addCreditRequest = AddCreditRequest(
            123L,
            "user1",
            BigDecimal(BigInteger.TEN),
            "2022-09-20 00:00:00",
            "2023-09-19 23:59:59")

        val mockCredit = Credit(
            addCreditRequest.id,
            addCreditRequest.userId,
            addCreditRequest.amount,
            dateFormatter.parse(addCreditRequest.startDate),
            dateFormatter.parse(addCreditRequest.expireDate!!))
        val creditCaptor = argumentCaptor<Credit>()
        Mockito.doReturn(mockCredit).`when`(creditRepository).save(creditCaptor.capture())

        val mockCreditOperation = AddCreditOperation(mockCredit, addCreditRequest.amount)
        val creditOperationCaptor = argumentCaptor<CreditOperation>()
        Mockito.doReturn(mockCreditOperation).`when`(creditOperationRepository).save(creditOperationCaptor.capture())

        //when
        creditService.addCredit(addCreditRequest)

        //then
        val savedCredit = creditCaptor.firstValue
        val savedCreditOperation = creditOperationCaptor.firstValue
        MatcherAssert.assertThat(savedCreditOperation.credit,Matchers.sameInstance(mockCredit))
        MatcherAssert.assertThat(savedCreditOperation.javaClass,Matchers.equalTo(AddCreditOperation::class.java))
    }

    @Test
    fun `it should create a new credit with add credit operation when add credit request received (spy and mock version)`() {
        //given
        val creditRepository = Mockito.mock(CreditRepository::class.java)
        val creditOperationRepository = Mockito.mock(CreditOperationRepository::class.java)
        val dateFormatter = DateFormatter()
        val timeFactory = TimeFactory()
        val creditService = Mockito.spy(CreditService(creditRepository,creditOperationRepository, dateFormatter, timeFactory))

        val addCreditRequest = AddCreditRequest(
            123L,
            "user1",
            BigDecimal(BigInteger.TEN),
            "2022-09-20 00:00:00",
            "2023-09-19 23:59:59")

        val mockCredit = Credit(
            addCreditRequest.id,
            addCreditRequest.userId,
            addCreditRequest.amount,
            dateFormatter.parse(addCreditRequest.startDate),
            dateFormatter.parse(addCreditRequest.expireDate!!))
        Mockito.doReturn(mockCredit).`when`(creditService).createCredit(addCreditRequest)
        Mockito.doReturn(mockCredit).`when`(creditRepository).save(mockCredit)

        val mockCreditOperation = AddCreditOperation(mockCredit, addCreditRequest.amount)
        Mockito.doReturn(mockCreditOperation).`when`(creditService).createAddCreditOperation(mockCredit)
        Mockito.doReturn(mockCreditOperation).`when`(creditOperationRepository).save(mockCreditOperation)

        //when
        creditService.addCredit(addCreditRequest)

        //then
        Mockito.verify(creditRepository).save(mockCredit)
        Mockito.verify(creditOperationRepository).save(mockCreditOperation)
        MatcherAssert.assertThat(mockCreditOperation.credit,Matchers.sameInstance(mockCredit))
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