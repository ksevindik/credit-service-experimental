package com.example.creditservice.model

import java.math.BigDecimal
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated

@Entity
@DiscriminatorValue("use")
class UseCreditOperation(
    credit: Credit,
    amount:BigDecimal,
    val paymentAttemptId:String,
    @Enumerated(EnumType.STRING) var status:CreditUsageStatus = CreditUsageStatus.Reserved) : CreditOperation(credit, amount) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as UseCreditOperation

        if (paymentAttemptId != other.paymentAttemptId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + paymentAttemptId.hashCode()
        return result
    }
}