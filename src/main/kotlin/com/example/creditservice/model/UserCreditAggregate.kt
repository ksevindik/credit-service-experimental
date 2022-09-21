package com.example.creditservice.model

import com.example.creditservice.repository.CreditOperationRepository
import com.example.creditservice.repository.CreditRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.Date

class UserCreditAggregate(
    /*
    candidate names for this aggregate:
    UserCreditConsumption
    UserCreditHistory
    UserCreditActivity
     */
    private val userId:String,
    private val instant:Instant,
    private val creditRepository: CreditRepository,
    private val creditOperationRepository: CreditOperationRepository) {

    fun getAvailableCreditAmount(): BigDecimal {
        val availableCredits = findAvailableCredits()
        return calculateAvailableCreditAmount(availableCredits)
    }

    private fun findAvailableCredits(): List<Credit> {
        return creditRepository.findAvailableCredits(userId, Date.from(instant))
    }

    private fun calculateAvailableCreditAmount(availableCredits: List<Credit>) =
        availableCredits.sumOf { it.amount }

    fun addCredit(requestId: Long, amount: BigDecimal, startDate: Date = Date(), expireDate: Date?) {
        var credit = Credit(requestId,userId,amount,startDate,expireDate)
        credit = creditRepository.save(credit)
        val addCreditOperation = AddCreditOperation(credit,amount)
        creditOperationRepository.save(addCreditOperation)
    }

    fun reserveCreditAmount(reserveAmount: BigDecimal, paymentAttemptId: String) {
        val availableCredits = findAvailableCredits()
        val availableCreditAmount = calculateAvailableCreditAmount(availableCredits)
        if(availableCreditAmount.compareTo(reserveAmount) < 0) {
            throw IllegalStateException("Available credit amount is not enough to satisfy reserve amount")
        }
        var remainingAmount = reserveAmount
        for(credit in availableCredits) {
            val usedAmount = credit.consume(remainingAmount)
            remainingAmount = remainingAmount.subtract(usedAmount)
            creditRepository.save(credit)
            creditOperationRepository.save(UseCreditOperation(credit,usedAmount, paymentAttemptId))
            if(remainingAmount.compareTo(BigDecimal.ZERO) == 0) break
        }
    }

    fun captureCredits(paymentAttemptId: String) {
        val useCreditOperations = creditOperationRepository.findByPaymentAttemptId(paymentAttemptId)
        useCreditOperations.forEach {
            it.status = CreditUsageStatus.Captured
            creditOperationRepository.save(it)
        }
    }

    fun revertCredits(paymentAttemptId: String) {
        val useCreditOperations = creditOperationRepository.findByPaymentAttemptId(paymentAttemptId)
        useCreditOperations.forEach {
            it.status = CreditUsageStatus.Reverted
            it.credit.amount = it.credit.amount.add(it.amount)
            creditOperationRepository.save(it)
            creditRepository.save(it.credit)
        }
    }
}