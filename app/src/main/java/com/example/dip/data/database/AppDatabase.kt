package com.example.dip.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.dip.data.CurrencyEntity
import com.example.dip.data.dao.CurrencyDao

@Database(entities = [CurrencyEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun currencyDao(): CurrencyDao
}