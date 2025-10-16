package com.example.dip.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currencies")
data class CurrencyEntity(
    @PrimaryKey val charCode: String,
    val name: String,
    val value: Double,
    val nominal: Int,
    val previous: Double
)