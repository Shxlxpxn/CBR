package com.example.dip.data.dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dip.data.CurrencyEntity

interface CurrencyDao {

    @Query("SELECT * FROM currencies")
    suspend fun getAll(): List<CurrencyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(currencies: List<CurrencyEntity>)

    @Query("DELETE FROM currencies")
    suspend fun deleteAll()
}