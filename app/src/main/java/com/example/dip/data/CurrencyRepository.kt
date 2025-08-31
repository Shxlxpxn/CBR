package com.example.dip.data

import com.example.dip.data.api.CbrApi
import com.example.dip.data.dao.CurrencyDao
import javax.inject.Inject

class CurrencyRepository @Inject constructor(
    private val api: CbrApi,
    private val currencyDao: CurrencyDao
) {

    suspend fun getCurrencies(forceRefresh: Boolean = false): List<CurrencyEntity> {
        return try {
            if (forceRefresh) {
                val remoteData = api.getCurrencyRates().valute
                val entities = remoteData.map {
                    CurrencyEntity(
                        charCode = it.charCode,
                        name = it.name,
                        value = it.valueDouble,
                        nominal = it.nominal,
                        previous = it.previousDouble
                    )
                }
                currencyDao.deleteAll()
                currencyDao.insertAll(entities)
                entities
            } else {
                currencyDao.getAll()
            }
        } catch (e: Exception) {
            currencyDao.getAll() // fallback на кэш
        }
    }
}


