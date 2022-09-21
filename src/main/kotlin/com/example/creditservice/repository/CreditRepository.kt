package com.example.creditservice.repository

import com.example.creditservice.model.Credit
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.Date

interface CreditRepository : CrudRepository<Credit,Long> {
    @Query("select c from Credit c where c.userId = :userId and c.expireDate > :expireDate order by c.expireDate asc")
    fun findAvailableCredits(userId: String, expireDate:Date) : List<Credit>
}