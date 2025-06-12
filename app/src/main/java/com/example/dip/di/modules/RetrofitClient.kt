package com.example.dip.di.modules

import CbrApi

object RetrofitClient {
    private const val BASE_URL = "https://www.cbr.ru/"

    val instance: CbrApi by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.simplexml.SimpleXmlConverterFactory.create())
            .build()
            .create(CbrApi::class.java)
    }
}