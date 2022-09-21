package com.example.creditservice.repository

import com.example.creditservice.model.CreditOperation
import com.example.creditservice.model.UseCreditOperation
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface CreditOperationRepository : CrudRepository<CreditOperation,Long> {
    @Query("select c from UseCreditOperation c where c.paymentAttemptId = :paymentAttemptId")
    fun findByPaymentAttemptId(paymentAttemptId:String): List<UseCreditOperation>

}