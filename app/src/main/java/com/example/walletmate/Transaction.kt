package com.example.walletmate

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.Date
import java.util.UUID

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(), // Unique ID
    val title: String,
    val type: String,
    val amount: Double,
    val category: String,
    val date: Date,
    val recipient: String?
) : Serializable