package com.example.dip.data


import retrofit2.http.GET


interface CbrApi {
    @GET("scripts/XML_daily.asp")
    suspend fun getCurrencyRates(): ValCurs
}