package com.example.creditservice.model

import java.math.BigDecimal
import javax.persistence.DiscriminatorColumn
import javax.persistence.DiscriminatorType
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Inheritance
import javax.persistence.InheritanceType
import javax.persistence.ManyToOne

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "op_type", discriminatorType = DiscriminatorType.STRING)
abstract class CreditOperation(
    @ManyToOne
    val credit:Credit,
    val amount:BigDecimal) : BaseEntity() {

    @Id
    @GeneratedValue
    var id:Long? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CreditOperation

        if (credit != other.credit) return false

        return true
    }

    override fun hashCode(): Int {
        return credit.hashCode()
    }

    override fun toString(): String {
        return "CreditOperation(credit=$credit, amount=$amount, id=$id)"
    }


}