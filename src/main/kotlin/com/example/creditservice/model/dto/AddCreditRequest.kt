package com.example.creditservice.model.dto

import java.math.BigDecimal

data class AddCreditRequest(
    val id:Long,
    val userId: String,
    val amount:BigDecimal,
    val startDate:String,
    val expireDate:String?=null)