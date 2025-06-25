package com.example.dip.di.modules

import android.content.Context
import androidx.room.Room
import com.example.dip.data.dao.CurrencyDao
import com.example.dip.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "currency_db"
        ).build()
    }

    @Provides
    fun provideCurrencyDao(database: AppDatabase): CurrencyDao {
        return database.currencyDao()
    }
}