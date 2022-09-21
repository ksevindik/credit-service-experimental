package com.example.creditservice.model

import com.example.creditservice.repository.CreditOperationRepository
import com.example.creditservice.repository.CreditRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.Date

class UserCreditAggregate(
    private val userId:String,
    private val instant:Instant,
    private val creditRepository: CreditRepository,
    private val creditOperationRepository: CreditOperationRepository) {

    fun getAvailableCreditAmount(): BigDecimal {
        val availableCredits = creditRepository.findAvailableCredits(userId, Date.from(instant))
        return availableCredits.sumOf { it.amount }
    }

    fun addCredit(requestId: Long, amount: BigDecimal, startDate: Date = Date(), expireDate: Date?) {
        var credit = Credit(requestId,userId,amount,startDate,expireDate)
        credit = creditRepository.save(credit)
        val addCreditOperation = AddCreditOperation(credit,amount)
        creditOperationRepository.save(addCreditOperation)
    }

    fun reserveCreditAmount(reserveAmount: BigDecimal, paymentAttemptId: String) {
        val availableCredits = creditRepository.findAvailableCredits(userId, Date.from(instant))
        var remainingAmount = reserveAmount
        for(credit in availableCredits) {
            val pair = calculateUsedAndRemainingAmounts(credit.amount, remainingAmount)
            credit.amount = credit.amount.subtract(pair.first)
            remainingAmount = pair.second
            creditRepository.save(credit)
            creditOperationRepository.save(UseCreditOperation(credit,pair.first, paymentAttemptId))
            if(remainingAmount.equals(BigDecimal.ZERO)) break
        }
    }


    private fun calculateUsedAndRemainingAmounts(amount:BigDecimal, requestedAmount:BigDecimal) : Pair<BigDecimal,BigDecimal> {
        var remainingAmount = requestedAmount.subtract(amount)
        var usedAmount:BigDecimal? = null
        if(remainingAmount.toLong() <= 0) {
            usedAmount = requestedAmount
            remainingAmount = BigDecimal.ZERO
        } else {
            usedAmount = amount
        }
        return Pair(usedAmount,remainingAmount)
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