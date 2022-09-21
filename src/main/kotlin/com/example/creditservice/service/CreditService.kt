package com.example.creditservice.service

import com.example.creditservice.model.AddCreditOperation
import com.example.creditservice.model.Credit
import com.example.creditservice.model.UseCreditOperation
import com.example.creditservice.model.CreditUsageStatus
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

    fun addCredit(addCreditRequest: AddCreditRequest) {
        var credit = createAndSaveCredit(addCreditRequest)
        createAndSaveCreditOperation(credit)
    }

    private fun createAndSaveCredit(addCreditRequest: AddCreditRequest): Credit {
        var credit = createCredit(addCreditRequest)
        credit = creditRepository.save(credit)
        return credit
    }

    internal fun createCredit(addCreditRequest: AddCreditRequest) = Credit(
        addCreditRequest.id,
        addCreditRequest.userId,
        addCreditRequest.amount,
        dateFormatter.parse(addCreditRequest.startDate),
        if(addCreditRequest.expireDate != null) dateFormatter.parse(addCreditRequest.expireDate) else null
    )

    private fun createAndSaveCreditOperation(credit: Credit) {
        var creditOperation = createAddCreditOperation(credit)
        creditOperationRepository.save(creditOperation)
    }

    internal fun createAddCreditOperation(credit: Credit) = AddCreditOperation(credit, credit.amount)

    fun reserveCreditAmount(userId: String, reserveAmount: BigDecimal, paymentAttemptId: String) {
        val availableCredits = creditRepository.findAvailableCredits(userId, timeFactory.getCurrentTime())
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