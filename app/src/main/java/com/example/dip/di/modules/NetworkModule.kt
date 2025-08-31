package com.example.dip.di.modules

import com.example.dip.data.api.CbrApi
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class NetworkModule {
    private val BASE_URL = "https://www.cbr.ru/"

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.simplexml.SimpleXmlConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideCbrApi(retrofit: Retrofit): CbrApi =
        retrofit.create(CbrApi::class.java)
}
