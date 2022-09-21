package com.example.creditservice.model

import java.math.BigDecimal
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue("add")
class AddCreditOperation(credit:Credit, amount:BigDecimal) : CreditOperation(credit, amount) {
}