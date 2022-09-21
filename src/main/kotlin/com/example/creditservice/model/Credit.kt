package com.example.creditservice.model

import java.math.BigDecimal
import java.util.Date
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToMany

@Entity
class Credit(
    @Id
    val id:Long,

    val userId:String,

    var amount:BigDecimal = BigDecimal(0),

    val startDate:Date = Date(),

    val expireDate:Date? = null
) : BaseEntity() {
    @OneToMany(mappedBy = "credit")
    val creditOperations:List<CreditOperation> = listOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Credit

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Credit(id=$id, userId='$userId', amount=$amount, startDate=$startDate, expireDate=$expireDate)"
    }

    fun consume(requestedAmount:BigDecimal) : BigDecimal {
        var usedAmount : BigDecimal? = null
        if(this.amount.compareTo(requestedAmount) > 0) {
            usedAmount = requestedAmount
            this.amount = this.amount.subtract(requestedAmount)
        } else {
            usedAmount = this.amount
            this.amount = BigDecimal.ZERO
        }
        return usedAmount
    }
}