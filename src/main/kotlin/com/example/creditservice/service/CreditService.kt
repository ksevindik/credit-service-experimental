package com.example.creditservice.service

import com.example.creditservice.model.UserCreditAggregate
import com.example.creditservice.model.dto.AddCreditRequest
import com.example.creditservice.repository.CreditOperationRepository
import com.example.creditservice.repository.CreditRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional
class CreditService(
    private val creditRepository: CreditRepository,
    private val creditOperationRepository: CreditOperationRepository,
    private val dateFormatter: DateFormatter,
    private val timeFactory:TimeFactory) {

    private fun createUserCreditAggregate(userId:String) : UserCreditAggregate {
        return UserCreditAggregate(
            userId,
            timeFactory.getCurrentTime().toInstant(),
            creditRepository,
            creditOperationRepository)
    }

    fun addCredit(addCreditRequest: AddCreditRequest) {
        val aggregate = createUserCreditAggregate(addCreditRequest.userId)
        aggregate.addCredit(
            addCreditRequest.id,
            addCreditRequest.amount,
            dateFormatter.parse(addCreditRequest.startDate),
            if(addCreditRequest.expireDate != null) dateFormatter.parse(addCreditRequest.expireDate) else null)
    }

    fun reserveCreditAmount(userId: String, reserveAmount: BigDecimal, paymentAttemptId: String) {
        val aggregate = createUserCreditAggregate(userId)
        aggregate.reserveCreditAmount(reserveAmount, paymentAttemptId)
    }

    fun captureCredits(userId: String, paymentAttemptId: String) {
        val aggregate = createUserCreditAggregate(userId)
        aggregate.captureCredits(paymentAttemptId)
    }

    fun revertCredits(userId: String, paymentAttemptId: String) {
        val aggregate = createUserCreditAggregate(userId)
        aggregate.revertCredits(paymentAttemptId)
    }
}